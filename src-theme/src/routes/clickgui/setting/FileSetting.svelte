<script lang="ts">
    import type {FileSetting, ModuleSetting} from "../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {openFileDialog} from "../../../integration/rest";

    export let setting: ModuleSetting;

    const cSetting = setting as FileSetting;
    let selecting = false

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }

    async function selectFile() {
        selecting = true;

        let file = await openFileDialog({
            mode: cSetting.dialogMode,
            supportedExtensions: cSetting.supportedExtensions
        })

        selecting = false;
        if (!file.cancelled) {
            cSetting.value = file.file;
            handleChange()
        }
    }

    async function browseFile() {

    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="setting">
    <div class="name">{spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>

    <div class="value">
        <span class="path" class:muted={cSetting.value === undefined}>
          {cSetting.value ?? 'Not set'}
        </span>
        <div class="buttons">
            <div class="button" onclick={selectFile} class:disabled={selecting}>Select</div>
            <div class="button" onclick={browseFile} class:disabled={cSetting.value === undefined}>Browse</div>
        </div>
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0 2px 0;
    display: grid;
    grid-template-areas:
    "a c"
    "d c";
    grid-template-columns: 1fr max-content;
    column-gap: 5px;

    min-height: 46px;
  }

  .name {
    grid-area: a;
  }

  .value {
    display: contents;
  }

  .path {
    grid-area: d;
    justify-self: start;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .buttons {
    grid-area: c;
    justify-self: end;
    display: flex;
    place-self: center;
    gap: 5px;
  }

  .button {
    background-color: $accent-color;
    padding: 3px 5px;
    cursor: pointer;
    display: flex;
    align-items: center;
    align-content: center;
    border-radius: 3px;
  }

  .disabled {
    opacity: 0.7;
    pointer-events: none;
  }

  .value,
  .setting {
    color: $clickgui-text-color;
    font-weight: 500;
    font-size: 12px;
  }

  .name {
    grid-area: a;
    font-weight: 500;
  }

  .muted {
    color: $clickgui-text-dimmed-color;
  }
</style>
