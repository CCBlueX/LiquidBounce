<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {BlocksSetting, ModuleSetting} from "../../../../integration/types";
    import {getRegistries} from "../../../../integration/rest";
    import Block from "./Block.svelte";
    import VirtualList from "./VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as BlocksSetting;

    interface TBlock {
        name: string;
        identifier: string;
    }

    const dispatch = createEventDispatcher();
    let blocks: TBlock[] = [];
    let renderedBlocks: TBlock[] = blocks;
    let searchQuery = "";

    $: {
        let filteredBlocks = blocks;
        if (searchQuery) {
            filteredBlocks = filteredBlocks.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedBlocks = filteredBlocks;
    }

    onMount(async () => {
        let b = (await getRegistries()).blocks;

        if (b !== undefined) {
            blocks = b.sort((a, b) => a.identifier.localeCompare(b.identifier));
        }
    });

    function handleBlockToggle(e: CustomEvent<{ identifier: string, enabled: boolean }>) {
        if (e.detail.enabled) {
            cSetting.value = [...cSetting.value, e.detail.identifier];
        } else {
            cSetting.value = cSetting.value.filter(b => b !== e.detail.identifier);
        }

        setting = {...cSetting};
        dispatch("change");
    }

    function selectAll() {
        if (searchQuery.trim() === '') {
            return;
        }
        for (const block of renderedBlocks) {
            if (!cSetting.value.includes(block.identifier)) {
                cSetting.value.push(block.identifier);
            }
        }
        setting = {...cSetting};
        dispatch("change");
    }

    function deselectAll() {
        cSetting.value = cSetting.value.filter(id => !renderedBlocks.some(block => block.identifier === id));
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery} spellcheck="false">
    <div class="action-buttons">
        <button on:click={selectAll}>Select All</button>
        <button on:click={deselectAll}>Deselect All</button>
    </div>
    <div class="results">
        <VirtualList items={renderedBlocks} let:item>
            <Block identifier={item.identifier} name={item.name} enabled={cSetting.value.includes(item.identifier)}
                   on:toggle={handleBlockToggle}/>
        </VirtualList>
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
  }

  .results {
    height: 200px;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .name {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 500;
    margin-bottom: 5px;
  }

  .search-input {
    width: 100%;
    border: none;
    border-bottom: solid 1px $accent-color;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    padding: 5px;
    color: $clickgui-text-color;
    margin-bottom: 5px;
    background-color: rgba($clickgui-base-color, .36);
  }

  .action-buttons {
    display: flex;
    gap: 5px;
    margin-bottom: 5px;

    button {
      flex: 1 1 50%;
      background-color: rgba($accent-color, 0.8);
      color: $clickgui-text-color;
      border: none;
      padding: 6px 0;
      font-size: 12px;
      border-radius: 4px;
      cursor: pointer;
      transition: background-color 0.2s;

      &:hover {
        background-color: rgba($accent-color, 1);
      }
    }

    .warning {
      background-color: rgba(255, 165, 0, 0.2);
      color: #ffa500;
      padding: 8px;
      border-radius: 4px;
      margin-bottom: 5px;
      font-size: 12px;
    }
  }

</style>
