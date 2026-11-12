<script lang="ts">
    import type {KeySetting, ModuleSetting} from "../../../integration/types";
    import SettingRow from "./common/SettingRow.svelte";
    import Keycap from "./bind/Keycap.svelte";
    import {keyName} from "./bind/key_names.svelte";
    import {createEventDispatcher} from "svelte";
    import {listen} from "../../../integration/ws";
    import type {KeyboardKeyEvent, MouseButtonEvent} from "../../../integration/events";
    import {isClickGuiScreen, UNKNOWN_KEY} from "../../../util/utils";
    import {escapeLayer} from "../../../integration/escape";

    export let setting: ModuleSetting;

    const cSetting = setting as KeySetting;

    const dispatch = createEventDispatcher();

    let isHovered = false;
    let binding = false;

    async function toggleBinding() {
        if (binding) {
            cSetting.value = UNKNOWN_KEY;
        }

        binding = !binding;

        setting = {...cSetting};

        dispatch("change");
    }

    const layer = escapeLayer(toggleBinding);
    $: layer.setOpen(binding);

    listen("keyboardKey", async (e: KeyboardKeyEvent) => {
        if (!isClickGuiScreen(e.screen)) {
            return;
        }

        if (!binding) {
            return;
        }

        binding = false;

        if (e.keyCode !== 27 /* SDLK_ESCAPE */) {
            cSetting.value = e.key;
        } else {
            cSetting.value = UNKNOWN_KEY;
        }

        setting = {...cSetting};

        dispatch("change");
    });

    listen("mouseButton", async (e: MouseButtonEvent) => {
        if (!isClickGuiScreen(e.screen)) {
            return;
        }

        if (!binding || (e.button === 1 /* SDL_BUTTON_LEFT */ && isHovered)) {
            return;
        }

        binding = false;

        cSetting.value = e.key;

        setting = {...cSetting};

        dispatch("change");
    })
</script>

<SettingRow {setting}>
    <Keycap
            {binding}
            muted={!binding && cSetting.value === UNKNOWN_KEY}
            on:click={toggleBinding}
            on:mouseenter={() => isHovered = true}
            on:mouseleave={() => isHovered = false}
    >
        {#if binding}
            Press a key
        {:else if cSetting.value === UNKNOWN_KEY}
            None
        {:else}
            {keyName(cSetting.value)}
        {/if}
    </Keycap>
</SettingRow>
