<script lang="ts">
    import type {FileSetting, ModuleSetting} from "../../../integration/types";
    import SettingRow from "./common/SettingRow.svelte";
    import TextButton from "./common/TextButton.svelte";
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

    $: fileName = cSetting.value.split(/[\\/]/).pop();
    $: hint = cSetting.dialogMode === "OPEN_FOLDER"
        ? "Choose folder"
        : `Choose ${cSetting.supportedExtensions?.join(", ") ?? "file"}`;

    function resetFile() {
        cSetting.value = '';
        handleChange();
    }
</script>

<SettingRow {setting}>
    <div class="file">
        {#if cSetting.value !== ""}
            <TextButton on:click={() => browsePath(cSetting.value)}>Open</TextButton>
            <button type="button" class="clear" title="Clear" aria-label="Clear" on:click={resetFile}></button>
        {/if}
        <button type="button" class="select" class:empty={cSetting.value === ""} title={cSetting.value} on:click={selectFile}>
            <img src="img/clickgui/icon-open-file.svg" alt="">
            <span class="file-name">{cSetting.value === "" ? hint : fileName}</span>
        </button>
    </div>
</SettingRow>

<style lang="scss">
  .file {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 4px;
    min-width: 0;
    max-width: 100%;
  }

  .select {
    display: flex;
    align-items: center;
    gap: 7px;
    min-width: 0;
    max-width: 220px;
    height: 26px;
    padding: 0 9px;
    border: none;
    border-radius: 3px;
    background-color: var(--clickgui-keycap-background-color);
    color: var(--clickgui-text-color);
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: ease background-color .15s;

    &:hover, &:focus-visible {
      background-color: var(--clickgui-button-secondary-hover-background-color);
    }

    &.empty {
      color: var(--clickgui-text-dimmed-color);
    }

    img {
      flex: none;
      width: 13px;
      height: 13px;
    }
  }

  .file-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .clear {
    flex: none;
    width: 20px;
    height: 20px;
    border: none;
    border-radius: 3px;
    background: url("/img/clickgui/icon-cross.svg") center / 9px no-repeat;
    opacity: .6;
    cursor: pointer;
    transition: ease opacity .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-control-hover-background-color);
    }
  }
</style>
