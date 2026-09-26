<script lang="ts">
    import type {FileSetting, ModuleSetting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {browsePath, openFileDialog} from "../../../integration/rest";
    import {createEventDispatcher} from "svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as FileSetting;

    let selecting = false;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    async function selectFile() {
        if (selecting) {
            return;
        }

        selecting = true;

        let file = await openFileDialog({
            mode: cSetting.dialogMode,
            supportedExtensions: cSetting.supportedExtensions
        });

        selecting = false;
        if (file.file !== undefined) {
            cSetting.value = file.file;
            handleChange();
        }
    }

    function resetFile() {
        cSetting.value = '';
        handleChange();
    }
</script>

<div class="setting">
    <div class="name">{spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>

    <div class="body">
        <button class="button-select" on:click={selectFile}>{cSetting.value === "" ? "<empty>" : cSetting.value}</button>

        {#if cSetting.value !== ""}
            <button class="button-action" on:click={resetFile}>
                <img class="icon" src="img/clickgui/icon-reset.svg" alt="reset-file" title="Reset" />
            </button>

            <button class="button-action" on:click={() => browsePath(cSetting.value)}>
                <img class="icon" src="img/clickgui/icon-open-file.svg" alt="open-file" title="Open" />
            </button>
        {/if}
    </div>
</div>

<style lang="scss">

  .setting {
    padding: 7px 0;
  }

  .name {
    font-weight: 500;
    color: var(--clickgui-text-color);
    font-size: 12px;
    margin-bottom: 5px;
  }

  .body {
    display: flex;
    column-gap: 5px;
  }

  .button-action {
    cursor: pointer;
    background-color: transparent;
    border: none;
    align-items: center;

    .icon {
      height: 18px;
    }
  }

  .button-select {
    cursor: pointer;
    width: 100%;
    background-color: var(--clickgui-input-background-color);
    font-family: monospace;
    font-size: 12px;
    color: var(--clickgui-text-dimmed-color);
    border: none;
    border-bottom: solid 2px var(--clickgui-input-border-color);
    padding: 6px;
    border-radius: 3px;
    transition: ease border-color .2s;

    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    direction: rtl;
  }
</style>
