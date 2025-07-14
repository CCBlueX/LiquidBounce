<script lang="ts">
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import {slide} from "svelte/transition";
    import type {BlocksSetting, ModuleSetting} from "../../../../integration/types";
    import {getRegistries} from "../../../../integration/rest";
    import Block from "./Block.svelte";
    import VirtualList from "./VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import ExpandArrow from "../common/ExpandArrow.svelte";

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

        setting = { ...cSetting };
        dispatch("change");
    }

    let expanded = true;

    // --- Resizable List ---

    let height = 200; // Default height
    let isResizing = false;
    let startY = 0;
    let startHeight = 0;

    function onMouseDown(event: MouseEvent) {
        isResizing = true;
        startY = event.clientY;
        startHeight = height;

        window.addEventListener('mousemove', onMouseMove);
        window.addEventListener('mouseup', onMouseUp);
    }

    function onMouseMove(event: MouseEvent) {
        if (isResizing) {
            const dy = event.clientY - startY;
            height = Math.max(40, startHeight + dy); // Minimum height
        }
    }

    function onMouseUp() {
        isResizing = false;
        window.removeEventListener('mousemove', onMouseMove);
        window.removeEventListener('mouseup', onMouseUp);
    }

    onDestroy(() => {
        window.removeEventListener('mousemove', onMouseMove);
        window.removeEventListener('mouseup', onMouseUp);
    });
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
            <div class="results" style="height: {height}px;">
                <VirtualList items={renderedBlocks} let:item>
                    <Block identifier={item.identifier} name={item.name} enabled={cSetting.value.includes(item.identifier)} on:toggle={handleBlockToggle}/>
                </VirtualList>
                <!-- svelte-ignore a11y-no-static-element-interactions -->
                <div class="resizer" on:mousedown={onMouseDown}></div>
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
    position: relative;
    overflow-y: auto;
    overflow-x: hidden;

    .resizer {
      all: unset;
      z-index: 1;
      position: absolute;
      bottom: 0;
      width: 100%;
      background: $accent-color;
      cursor: ns-resize;
      height: 4px;
      border-radius: 2px;
    }
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
