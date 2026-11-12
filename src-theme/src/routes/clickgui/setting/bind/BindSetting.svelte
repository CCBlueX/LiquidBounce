<script lang="ts">
    import {createEventDispatcher, onDestroy} from "svelte";
    import type {BindAction, BindModifier, BindSetting, ModuleSetting} from "../../../../integration/types";
    import {waitMatches} from "../../../../integration/ws";
    import type {KeyboardKeyEvent, MouseButtonEvent} from "../../../../integration/events";
    import BindDisplay from "./BindDisplay.svelte";
    import Keycap from "./Keycap.svelte";
    import Segmented from "../common/Segmented.svelte";
    import SettingRow from "../common/SettingRow.svelte";
    import {isClickGuiScreen, UNKNOWN_KEY} from "../../../../util/utils";
    import {escapeLayer} from "../../../../integration/escape";

    /**
     * SDL keycodes of the modifier keys (see org.lwjgl.sdl.SDLKeycode)
     * https://wiki.libsdl.org/SDL3/SDL_Keycode
     */
    const KEY_TOKEN_TO_MODIFIERS: Record<number, BindModifier> = {
        1073742049: "Shift", 1073742053: "Shift", // SDLK_LSHIFT / SDLK_RSHIFT
        1073742048: "Control", 1073742052: "Control", // SDLK_LCTRL / SDLK_RCTRL
        1073742050: "Alt", 1073742054: "Alt", // SDLK_LALT / SDLK_RALT
        1073742051: "Super", 1073742055: "Super", // SDLK_LGUI / SDLK_RGUI
    } as const;

    /**
     * From Minecraft InputUtil.Type
     */
    const KEY_CODE_TO_MODIFIERS: Record<string, BindModifier> = {
        "key.keyboard.left.shift": "Shift", "key.keyboard.right.shift": "Shift",
        "key.keyboard.left.control": "Control", "key.keyboard.right.control": "Control",
        "key.keyboard.left.alt": "Alt", "key.keyboard.right.alt": "Alt",
        "key.keyboard.left.win": "Super", "key.keyboard.right.win": "Super",
    } as const;

    const ACTIONS: BindAction[] = ["Toggle", "Hold", "Smart"];

    export let setting: ModuleSetting;

    const cSetting = setting as BindSetting;

    const dispatch = createEventDispatcher();

    let isHovered = false;
    let binding = false;

    /**
     * Gets the next possible event which can be used as a bind.
     */
    const nextBindEvent = () => Promise.any([
        waitMatches("mouseButton", (e: MouseButtonEvent) =>
            isClickGuiScreen(e.screen) && !(e.button === 1 /* SDL_BUTTON_LEFT */ && isHovered)
        ),
        waitMatches("keyboardKey", (e: KeyboardKeyEvent) =>
            isClickGuiScreen(e.screen)
        ),
    ]);

    let addedModifiers = new Set<BindModifier>();

    /**
     * Tries to handle the event. If it's consumed
     * @return undefined if it's not a modifier, or else its key name and parsed modifier
     */
    const handleBindEventIfNotModifier = (event: MouseButtonEvent | KeyboardKeyEvent) => {
        if (Object.hasOwn(event, "keyCode")) {
            const e = event as KeyboardKeyEvent;
            // ESC is pressed (SDL keycode of SDLK_ESCAPE)
            if (e.keyCode === 27 /* SDLK_ESCAPE */) {
                handleActionChange(UNKNOWN_KEY);
                return;
            }

            const modifier = KEY_TOKEN_TO_MODIFIERS[e.keyCode];

            if (!modifier) {
                handleActionChange(e.key);
                return;
            }

            return {key: e.key, keyCode: e.keyCode, modifier};
        } else if (Object.hasOwn(event, "button")) {
            const e = event as MouseButtonEvent;
            handleActionChange(e.key);
        } else {
            throw new Error("Unexcepted event: " + JSON.stringify(event));
        }
    }

    let timeout: ReturnType<typeof setTimeout> | undefined = undefined;

    onDestroy(() => {
        if (timeout !== undefined) {
            clearTimeout(timeout);
        }
    });

    async function toggleBinding() {
        // Binding progress -> cancel it
        if (binding) {
            handleActionChange(UNKNOWN_KEY);
            return;
        }

        binding = true;

        let event = await nextBindEvent();
        // Promise doesn't support cancellation, so we need manual check
        if (!binding) return;

        let result = handleBindEventIfNotModifier(event);

        while (result) {
            if (timeout !== undefined) {
                clearTimeout(timeout);
            }
            const {key, modifier} = result;

            addedModifiers.add(modifier);
            addedModifiers = addedModifiers; // Trigger reactive update

            timeout = setTimeout(() => {
                if (binding) {
                    handleActionChange(key);
                }
                timeout = undefined;
            }, 1000);

            event = await nextBindEvent();

            if (!binding) return;

            result = handleBindEventIfNotModifier(event);
        }
    }

    function handleActionChange(newBoundKey: string) {
        addedModifiers.delete(KEY_CODE_TO_MODIFIERS[newBoundKey]); // We don't want Shift+RIGHT_SHIFT
        cSetting.value.boundKey = newBoundKey;
        cSetting.value.modifiers = Array.from(addedModifiers);
        addedModifiers.clear();
        binding = false;
        handleChange();
    }

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    const layer = escapeLayer(() => handleActionChange(UNKNOWN_KEY));
    $: layer.setOpen(binding);
</script>

<SettingRow {setting}>
    <div class="bind">
        {#if !binding && cSetting.value.boundKey !== UNKNOWN_KEY}
            <Segmented options={ACTIONS} bind:value={cSetting.value.action} on:change={handleChange}/>
        {/if}

        <Keycap
                {binding}
                muted={!binding && cSetting.value.boundKey === UNKNOWN_KEY}
                on:click={toggleBinding}
                on:mouseenter={() => isHovered = true}
                on:mouseleave={() => isHovered = false}
        >
            {#if !binding}
                {#if cSetting.value.boundKey === UNKNOWN_KEY}
                    None
                {:else}
                    <BindDisplay
                            bind:modifiers={cSetting.value.modifiers}
                            bind:boundKey={cSetting.value.boundKey}
                    />
                {/if}
            {:else if addedModifiers.size}
                <BindDisplay
                        bind:modifiers={addedModifiers}
                        boundKey="..."
                        literal={true}
                />
            {:else}
                Press a key
            {/if}
        </Keycap>
    </div>
</SettingRow>

<style lang="scss">
  .bind {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 4px 6px;
    min-width: 0;
  }
</style>
