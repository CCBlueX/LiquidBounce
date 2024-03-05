<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {ModuleSetting, BlocksSetting} from "../../../../integration/types";
    import {getRegistries} from "../../../../integration/rest";
    import Block from "./Block.svelte";
    import VirtualList from "./VirtualList.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as BlocksSetting;

    const dispatch = createEventDispatcher();
    let blocks: string[] = [];
    let renderedBlocks: string[] = blocks;
    let searchQuery = "";

    $: {
        let filteredBlocks = blocks;
        if (searchQuery) {
            filteredBlocks = filteredBlocks.filter(b => b.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedBlocks = filteredBlocks;
    }

    onMount(async () => {
        blocks = (await getRegistries()).blocks.sort((a, b) => a.localeCompare(b));
    });

    function handleBlockToggle(e: CustomEvent<{ identifier: string, enabled: boolean }>) {
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
    <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery}>
    <div class="results">
        <VirtualList items={renderedBlocks} autoScroll={false} let:item>
            <Block identifier={item} enabled={cSetting.value.includes(item)} on:toggle={handleBlockToggle}/>
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
    display: flex;
    flex-direction: column;
    row-gap: 5px;
  }

  .search-input {
    background-color: rgba($clickgui-base-color, .36);
    width: 100%;
    border: none;
    border-bottom: solid 1px $accent-color;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    padding: 5px;
    color: $clickgui-text-color;
    margin-bottom: 5px;
  }
</style>