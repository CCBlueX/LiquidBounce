<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, TextSetting,} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as TextSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <input type="text" class="value" spellcheck="false"
           placeholder={$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}
           bind:value={cSetting.value}
           on:input={handleChange}
    >
</div>

<style lang="scss">

  .setting {
    padding: 7px 0px;
  }

  .name {
    font-weight: 500;
    color: var(--clickgui-text-color);
    font-size: 12px;
    margin-bottom: 5px;
  }

  .value {
    width: 100%;
    background-color: var(--clickgui-input-background-color);
    font-family: monospace;
    font-size: 12px;
    color: var(--clickgui-text-color);
    border: 1px solid var(--accent-border-color);
    padding: 7px;
    border-radius: var(--radius-xs);
    transition: border-color var(--duration-fast) var(--ease-standard),
    box-shadow var(--duration-fast) var(--ease-standard);

    &:focus-visible {
      border-color: var(--clickgui-input-border-color);
      box-shadow: 0 0 0 2px var(--accent-subtle-background-color);
    }

    &::-webkit-scrollbar {
      background-color: transparent;
    }
  }
</style>
