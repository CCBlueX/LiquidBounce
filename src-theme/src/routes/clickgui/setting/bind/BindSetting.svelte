<script lang="ts">
    import {createEventDispatcher, onDestroy} from "svelte";
    import type {BindModifier, BindSetting, ModuleSetting} from "../../../../integration/types";
    import {waitMatches} from "../../../../integration/ws";
    import type {KeyboardKeyEvent, MouseButtonEvent} from "../../../../integration/events";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import BindDisplay from "./BindDisplay.svelte";
    import SwitchBindAction from "./SwitchBindAction.svelte";
    import {isClickGuiScreen, UNKNOWN_KEY} from "../../../../util/utils";

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
</script>

<div class="setting" class:has-value={cSetting.value.boundKey !== UNKNOWN_KEY}>
    <button
            class="change-bind"
            on:click={toggleBinding}
            on:mouseenter={() => isHovered = true}
            on:mouseleave={() => isHovered = false}
    >
        <span class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</span>

        {#if cSetting.value.boundKey !== UNKNOWN_KEY}
            <div class="action">
                <SwitchBindAction
                        choices={["Toggle", "Hold", "Smart"]}
                        bind:chosen={cSetting.value.action}
                        onchange={handleChange}
                />
            </div>
        {/if}

        <span class="bind">
            {#if !binding}
                <BindDisplay
                        bind:modifiers={cSetting.value.modifiers}
                        bind:boundKey={cSetting.value.boundKey}
                />
            {:else if addedModifiers.size}
                <BindDisplay
                        bind:modifiers={addedModifiers}
                        boundKey="..."
                        literal={true}
                />
            {:else}
                <span>Press any key...</span>
            {/if}
        </span>
    </button>
</div>

<style lang="scss">

  .name {
    text-align: center;
    pointer-events: none;
  }

  .action {
    position: absolute;
    top: 4px;
    right: 4px;
  }

  .bind {
    display: flex;
    justify-content: center;
  }

  .setting {
    padding: 7px 0;
  }

  .change-bind {
    background-color: transparent;
    border: solid 2px var(--accent-color);
    border-radius: 3px;
    cursor: pointer;
    padding: 4px;
    font-weight: 500;
    color: var(--clickgui-text-color);
    font-size: 12px;
    font-family: "Inter", sans-serif;
    width: 100%;
    position: relative;
  }
</style>
