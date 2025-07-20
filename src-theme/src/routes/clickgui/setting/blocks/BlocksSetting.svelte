<script lang="ts">
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import {slide} from "svelte/transition";
    import type {BlocksSetting, ModuleSetting} from "../../../../integration/types";
    import {getRegistries} from "../../../../integration/rest";
    import Block from "./Block.svelte";
    import VirtualList from "./VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {setItem} from "../../../../integration/persistent_storage";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as BlocksSetting;
    const thisPath = `${path}.${cSetting.name}`;

    interface TBlock {
        name: string;
        identifier: string;
    }

    const dispatch = createEventDispatcher();
    let blocks: TBlock[] = [];
    let renderedBlocks: TBlock[] = blocks;
    let searchQuery = "";
    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

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
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" class:expanded on:contextmenu|preventDefault={() => expanded = !expanded}>
        <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <ExpandArrow bind:expanded/>
    </div>
    {#if expanded}
        <div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}}>
            <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery} spellcheck="false">
            <div class="results">
                <VirtualList items={renderedBlocks} let:item>
                    <Block identifier={item.identifier} name={item.name}
                           enabled={cSetting.value.includes(item.identifier)} on:toggle={handleBlockToggle}/>
                </VirtualList>
            </div>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
  }

  .head {
    display: flex;
    justify-content: space-between;
    transition: ease margin-bottom .2s;

    &.expanded {
      margin-bottom: 10px;
    }

    .name {
      color: $clickgui-text-color;
      font-size: 12px;
      font-weight: 500;
    }
  }

  .results {
    height: 200px;
    overflow-y: auto;
    overflow-x: hidden;
    min-height: 100px;
    max-height: 500px;
    position: relative;
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