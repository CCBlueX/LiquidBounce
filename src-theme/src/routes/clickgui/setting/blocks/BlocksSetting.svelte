<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {BlocksSetting, ModuleSetting} from "../../../../integration/types";
    import {getRegistries} from "../../../../integration/rest";
    import Block from "./Block.svelte";
    import VirtualList from "./VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as BlocksSetting;

    interface Block {
        name: string;
        identifier: string;
    }

    const dispatch = createEventDispatcher();
    let blocks: Block[] = [];
    let renderedBlocks: Block[] = blocks;
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
        console.log(e);
        if (e.detail.enabled) {
            cSetting.value = [...cSetting.value, e.detail.identifier];
        } else {
            cSetting.value = cSetting.value.filter(b => b !== e.detail.identifier);
        }

        setting = { ...cSetting };
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery}>
    <div class="results">
        <VirtualList items={renderedBlocks} let:item>
            <Block identifier={item.identifier} name={item.name} enabled={cSetting.value.includes(item.identifier)} on:toggle={handleBlockToggle}/>
        </VirtualList>
    </div>
</div>

<style lang="scss">
  @import "../../../../colors.scss";

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
</style>
