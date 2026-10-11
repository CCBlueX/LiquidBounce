import assert from "node:assert/strict";
import {readFileSync} from "node:fs";
import {test} from "node:test";
import vm from "node:vm";
import ts from "typescript";

const root = new URL("../", import.meta.url);
const screen = {class: "net.ccbluex.liquidbounce.ClickGui", title: "ClickGUI"};
const unknown = "key.keyboard.unknown";

function evaluate(source, context, imports, filename) {
    const module = {exports: {}};
    const output = ts.transpileModule(source, {
        compilerOptions: {module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022},
        fileName: filename,
    }).outputText;
    const run = vm.runInContext(`(function(require, module, exports) { ${output} })`, context, {filename});
    run(name => {
        assert.ok(name in imports, `Unmocked import: ${name}`);
        return imports[name];
    }, module, module.exports);
    return module.exports;
}

function fixture() {
    const destroyHooks = [];
    const timers = new Map();
    let timerId = 0;
    let socket;
    const setting = {name: "Bind", value: {boundKey: unknown, modifiers: [], action: "Toggle"}};
    let changes = 0;
    class WebSocket {
        constructor() { socket = this; }
    }
    const context = vm.createContext({
        console: {log() {}, error() {}},
        WebSocket, AbortController,
        setInterval() {},
        setTimeout(callback) { const id = ++timerId; timers.set(id, callback); return id; },
        clearTimeout(id) { timers.delete(id); },
        __setting: setting,
    });
    const ws = evaluate(readFileSync(new URL("src/integration/ws.ts", root), "utf8"), context, {
        "./host": {WS_BASE: "ws://test.invalid"},
        "svelte": {onDestroy: hook => destroyHooks.push(hook)},
    }, "ws.ts");
    const component = readFileSync(new URL("src/routes/clickgui/setting/bind/BindSetting.svelte", root), "utf8");
    // Run the component's real script with a supplied prop and mocked Svelte lifecycle/dispatch.
    const script = component.match(/<script lang="ts">([\s\S]*?)<\/script>/)[1]
        .replace("export let setting: ModuleSetting;", "export let setting: ModuleSetting = globalThis.__setting;");
    const bind = evaluate(script + "\nexport {toggleBinding};", context, {
        "svelte": {createEventDispatcher: () => () => changes++, onDestroy: hook => destroyHooks.push(hook)},
        "../../../../integration/ws": ws,
        "../../../../theme/theme_config": {},
        "./BindDisplay.svelte": {},
        "./SwitchBindAction.svelte": {},
        "../../../../util/utils": {UNKNOWN_KEY: unknown, isClickGuiScreen: value => value?.title === "ClickGUI"},
    }, "BindSetting.svelte");
    return {
        ws, bind, setting, timers,
        changes: () => changes,
        emit(name, event) { socket.onmessage({data: JSON.stringify({name, event})}); },
        destroy() { destroyHooks.forEach(hook => hook()); },
        fireTimers() {
            const callbacks = [...timers.values()];
            timers.clear();
            callbacks.forEach(callback => callback());
        },
    };
}

const flush = async () => { for (let i = 0; i < 10; i++) await Promise.resolve(); };
async function settled(promise) {
    let done = false;
    promise.then(() => { done = true; }, () => { done = true; });
    await flush();
    assert.ok(done, "The event wait must settle after cancellation or completion");
    return promise;
}

const key = (keyCode, name) => ({keyCode, key: `key.keyboard.${name}`, action: 1, screen});
const mouse = button => ({button, key: `key.mouse.${button}`, action: 1, screen});

test("aborting a pending event wait removes its predicate", async () => {
    const f = fixture();
    const controller = new AbortController();
    let calls = 0;
    const pending = f.ws.waitMatches("keyboardKey", () => { calls++; return true; }, controller.signal);
    controller.abort();
    await assert.rejects(settled(pending), error => error.name === "AbortError");
    f.emit("keyboardKey", key(97, "a"));
    assert.equal(calls, 0);
});

test("an already aborted signal does not register an event wait", async () => {
    const f = fixture();
    const controller = new AbortController();
    controller.abort("cancelled");
    let calls = 0;
    await assert.rejects(settled(f.ws.waitMatches("keyboardKey", () => { calls++; return true; }, controller.signal)),
        reason => reason === "cancelled");
    f.emit("keyboardKey", key(97, "a"));
    assert.equal(calls, 0);
});

test("matched and failed predicates are removed", async () => {
    const f = fixture();
    let matchedCalls = 0;
    let failedCalls = 0;
    const matched = f.ws.waitMatches("keyboardKey", () => { matchedCalls++; return true; });
    const failed = f.ws.waitMatches("keyboardKey", () => { failedCalls++; throw new Error("predicate"); });
    f.emit("keyboardKey", key(97, "a"));
    await matched;
    await assert.rejects(failed, /predicate/);
    f.emit("keyboardKey", key(98, "b"));
    assert.equal(matchedCalls, 1);
    assert.equal(failedCalls, 1);
});

test("keyboard and mouse bindings each finish once", async () => {
    const f = fixture();
    const keyboard = f.bind.toggleBinding();
    f.emit("keyboardKey", key(97, "a"));
    await keyboard;
    assert.equal(f.setting.value.boundKey, "key.keyboard.a");
    f.emit("mouseButton", mouse(3));
    await flush();
    assert.equal(f.changes(), 1);

    const pointer = f.bind.toggleBinding();
    f.emit("mouseButton", mouse(3));
    await pointer;
    assert.equal(f.setting.value.boundKey, "key.mouse.3");
    f.emit("keyboardKey", key(98, "b"));
    await flush();
    assert.equal(f.changes(), 2);
});

test("cancelling and immediately restarting cannot revive the old binding", async () => {
    const f = fixture();
    const old = f.bind.toggleBinding();
    await f.bind.toggleBinding();
    const current = f.bind.toggleBinding();
    f.emit("keyboardKey", key(97, "a"));
    await settled(Promise.all([old, current]));
    assert.equal(f.setting.value.boundKey, "key.keyboard.a");
    assert.equal(f.changes(), 2, "one clear and one completed binding");
});

test("destroying a component stops a pending modifier timer and input waits", async () => {
    const f = fixture();
    const pending = f.bind.toggleBinding();
    f.emit("keyboardKey", key(1073742049, "left.shift"));
    await flush();
    assert.equal(f.timers.size, 1);
    f.destroy();
    await settled(pending);
    f.fireTimers();
    f.emit("keyboardKey", key(97, "a"));
    await flush();
    assert.equal(f.timers.size, 0);
    assert.equal(f.changes(), 0);
});

test("a modifier timeout leaves no stale binding when another session begins", async () => {
    const f = fixture();
    const first = f.bind.toggleBinding();
    f.emit("keyboardKey", key(1073742049, "left.shift"));
    await flush();
    f.fireTimers();
    await settled(first);
    assert.equal(f.setting.value.boundKey, "key.keyboard.left.shift");
    assert.deepEqual(Array.from(f.setting.value.modifiers), []);

    const second = f.bind.toggleBinding();
    f.emit("keyboardKey", key(97, "a"));
    await second;
    assert.equal(f.setting.value.boundKey, "key.keyboard.a");
    assert.equal(f.changes(), 2);
});

test("modifier combinations retain their modifiers and cancel the timeout", async () => {
    const f = fixture();
    const pending = f.bind.toggleBinding();
    f.emit("keyboardKey", key(1073742049, "left.shift"));
    await flush();
    f.emit("keyboardKey", key(97, "a"));
    await settled(pending);
    assert.equal(f.setting.value.boundKey, "key.keyboard.a");
    assert.equal(Array.from(f.setting.value.modifiers).join(","), "Shift");
    assert.equal(f.timers.size, 0);
    f.fireTimers();
    assert.equal(f.changes(), 1);
});
