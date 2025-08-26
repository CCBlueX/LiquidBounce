<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, TextSetting,} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {setTyping} from "../../../integration/rest";

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
           on:focusin={async () => await setTyping(true)}
           on:focusout={async () => await setTyping(false)}
    >
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0px;
  }

  .name {
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    margin-bottom: 5px;
  }

  .value {
    width: 100%;
    background-color: rgba($clickgui-base-color, .36);
    font-family: monospace;
    font-size: 12px;
    color: $clickgui-text-color;
    border: none;
    border-bottom: solid 2px $accent-color;
    padding: 5px;
    border-radius: 3px;
    transition: ease border-color .2s;

    &::-webkit-scrollbar {
      background-color: transparent;
    }
  }
</style>
