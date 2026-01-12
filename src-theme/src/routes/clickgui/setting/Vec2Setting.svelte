<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, Vec2Setting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;
    const cSetting = setting as Vec2Setting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <div class="input-group">
        <input type="number" class="value" spellcheck="false" placeholder="X" bind:value={cSetting.value.x}
               on:input={handleChange}/>
        <input type="number" class="value" spellcheck="false" placeholder="Y" bind:value={cSetting.value.y}
               on:input={handleChange}/>
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
  }

  .name {
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    margin-bottom: 5px;
  }

  .input-group {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    column-gap: 5px;

    input.value {
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
      appearance: textfield;

      &::-webkit-scrollbar {
        background-color: transparent;
      }

      /* Hide the number input spinner buttons */
      &::-webkit-outer-spin-button,
      &::-webkit-inner-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }
  }
</style>
