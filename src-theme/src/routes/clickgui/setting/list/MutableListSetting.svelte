<script lang="ts">
    import type {ListSetting, ModuleSetting} from "../../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {createEventDispatcher} from "svelte";
    import SettingButton from "../common/SettingButton.svelte";
    import RemovableItem from "../common/RemovableItem.svelte";

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
    <SettingButton value="Add value" on:click={addValueIndex} />
    {#if cSetting.value.length > 0}
        <div class="inputs">
            {#each cSetting.value as _, index}
                <RemovableItem on:remove={() => removeValueIndex(index)}>
                    <input type="text" class="value" spellcheck="false" placeholder={setting.name} bind:value={cSetting.value[index]}
                           on:input={handleChange}>
                </RemovableItem>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  .setting {
    padding: 7px 0;
  }

  .inputs {
    display: flex;
    flex-direction: column;
    row-gap: 10px;
    margin-top: 5px;
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
    border: none;
    border-bottom: solid 2px var(--clickgui-input-border-color);
    padding: 6px;
    border-radius: 3px;
    transition: ease border-color .2s;

    &::-webkit-scrollbar {
      background-color: transparent;
    }
  }
</style>
