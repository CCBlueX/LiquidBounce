<script lang="ts">
    import type {ListSetting, ModuleSetting} from "../../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import RemovableItem from "../common/RemovableItem.svelte";
    import SettingRow from "../common/SettingRow.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as ListSetting;

    const dispatch = createEventDispatcher();

    let newEntry = "";

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    function removeValueIndex(index: number) {
        cSetting.value.splice(index, 1);
        cSetting.value = cSetting.value;
        handleChange();
    }

    function addEntry() {
        if (newEntry === "") {
            return;
        }

        cSetting.value = [...cSetting.value, newEntry];
        newEntry = "";
        handleChange();
    }
</script>

<SettingRow {setting}>
    <span class="count">{cSetting.value.length} {cSetting.value.length === 1 ? "entry" : "entries"}</span>

    <svelte:fragment slot="below">
        {#each cSetting.value as _, index}
            <RemovableItem on:remove={() => removeValueIndex(index)}>
                <input type="text" class="value" spellcheck="false" aria-label="Entry {index + 1}"
                       bind:value={cSetting.value[index]}
                       on:input={handleChange}>
            </RemovableItem>
        {/each}
        <form class="add" on:submit|preventDefault={addEntry}>
            <input type="text" class="new-entry" spellcheck="false" placeholder="Add entry" aria-label="Add entry"
                   bind:value={newEntry}>
            {#if newEntry !== ""}
                <button type="submit" class="confirm">Add</button>
            {/if}
        </form>
    </svelte:fragment>
</SettingRow>

<style lang="scss">
  @use "../common/underlined-input" as *;

  .count {
    font-size: 11px;
    color: var(--clickgui-text-muted-color);
    white-space: nowrap;
  }

  .value {
    flex: 1;
    min-width: 0;
    height: 28px;
    border: none;
    background: none;
    color: var(--clickgui-text-color);
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
  }

  .add {
    position: relative;
    padding: 1px 0 8px;
  }

  .new-entry {
    @include underlined-input;
    width: 100%;
  }

  .confirm {
    position: absolute;
    top: 5px;
    right: 4px;
    height: 20px;
    padding: 0 6px;
    border: none;
    border-radius: 3px;
    background: none;
    color: var(--accent-color);
    font-family: "Inter", sans-serif;
    font-size: 11px;
    font-weight: 600;
    cursor: pointer;
    transition: ease background-color .15s;

    &:hover, &:focus-visible {
      background-color: var(--clickgui-control-hover-background-color);
    }
  }
</style>
