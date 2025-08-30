<script lang="ts">
    import type {ListSetting, ModuleSetting} from "../../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {createEventDispatcher} from "svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as ListSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    function removeValueIndex(index: number) {
        cSetting.value.splice(index, 1);
        cSetting.value = cSetting.value;
        handleChange();
    }

    function addValueIndex() {
        cSetting.value = ["", ...cSetting.value];
        handleChange();
    }
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <button class="button-add" on:click={addValueIndex}>Add value</button>
    {#if cSetting.value.length > 0}
        <div class="inputs">
            {#each cSetting.value as _, index}
                <div class="input-wrapper">
                    <input type="text" class="value" spellcheck="false" placeholder={setting.name} bind:value={cSetting.value[index]}
                           on:input={handleChange}>
                    <button class="button-remove" title="Remove" on:click={() => removeValueIndex(index)}>
                        <img src="img/clickgui/icon-cross.svg" alt="remove">
                    </button>
                </div>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../colors" as *;

  .input-wrapper {
    display: grid;
    grid-template-columns: 1fr max-content;
    column-gap: 5px;
    align-items: center;
  }

  .button-remove {
    background-color: transparent;
    border: none;
    cursor: pointer;
  }

  .setting {
    padding: 7px 0px;
  }

  .inputs {
    display: flex;
    flex-direction: column;
    row-gap: 10px;
    margin-top: 5px;
  }

  .name {
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    margin-bottom: 5px;
  }

  .button-add {
    font-family: monospace;
    font-size: 12px;
    color: $clickgui-text-color;
    background-color: $accent-color;
    border: none;
    padding: 6px 10px;
    border-radius: 3px;
    width: 100%;
    cursor: pointer;
    transition: ease background-color .2s;

    &:hover {
        background-color: color.adjust(color.adjust($accent-color, $saturation: -30%), $lightness: -10%);
    }
  }

  .value {
    width: 100%;
    background-color: rgba($clickgui-base-color, .36);
    font-family: monospace;
    font-size: 12px;
    color: $clickgui-text-color;
    border: none;
    border-bottom: solid 2px $accent-color;
    padding: 6px;
    border-radius: 3px;
    transition: ease border-color .2s;

    &::-webkit-scrollbar {
      background-color: transparent;
    }
  }
</style>
