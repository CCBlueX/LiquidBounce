<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {KeySetting, ModuleSetting} from "../../../integration/types";
    import {listen} from "../../../integration/ws";
    import {getPrintableKeyName} from "../../../integration/rest";
    import type {KeyboardKeyEvent} from "../../../integration/events";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as KeySetting;

    const dispatch = createEventDispatcher();

    let binding = false;
    let printableKeyName = "";

    async function updatePrintableKeyName() {
        if (cSetting.value === -1) {
            return;
        }
        printableKeyName = (await getPrintableKeyName(cSetting.value)).localized;
    }

    listen("keyboardKey", async (e: KeyboardKeyEvent) => {
        if (!binding) {
            return;
        }

        binding = false;

        if (e.keyCode !== 256) {
            cSetting.value = e.keyCode;
        } else {
            cSetting.value = -1;
        }
        await updatePrintableKeyName();

        setting = {...cSetting};

        dispatch("change");
    });

    async function toggleBinding() {
        if (binding) {
            cSetting.value = -1;
            await updatePrintableKeyName();
        }

        binding = !binding;

        setting = {...cSetting};

        dispatch("change");
    }

    onMount(async () => {
        await updatePrintableKeyName();
    });
</script>

<div class="setting">
    <button class="change-bind" on:click={toggleBinding}>
        {#if !binding}
            <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}:</div>
            
            {#if cSetting.value === -1}
                <span class="none">None</span>
            {:else}
                <span>{printableKeyName}</span>
            {/if}
        {:else}
            <span>Press any key</span>
        {/if}
    </button>
</div>

<style lang="scss">
  @import "../../../colors.scss";

  .setting {
    padding: 7px 0px;
  }

  .change-bind {
    background-color: transparent;
    border: solid 2px $accent-color;
    border-radius: 3px;
    cursor: pointer;
    padding: 5px;
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    font-family: "Inter", sans-serif;
    width: 100%;
    display: flex;
    justify-content: center;
    column-gap: 5px;

    .name {
      font-weight: 500;
    }

    .none {
      color: $clickgui-text-dimmed-color;
    }
  }
</style>
