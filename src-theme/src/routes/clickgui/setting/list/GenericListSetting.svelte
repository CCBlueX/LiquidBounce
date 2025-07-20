<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {slide} from "svelte/transition";
    import type {ListSetting, ModuleSetting, NamedItem} from "../../../../integration/types";
    import VirtualList from "../list/VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {setItem} from "../../../../integration/persistent_storage";
    import ListItem from "./ListItem.svelte";

    export let setting: ModuleSetting;
    export let path: string;
    export let items: NamedItem[];

    const cSetting = setting as ListSetting;
    const thisPath = `${path}.${cSetting.name}`;

    const dispatch = createEventDispatcher();
    let renderedItems: NamedItem[] = items;
    let searchQuery = "";
    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

    $: {
        let filteredItems = items;
        if (searchQuery) {
            filteredItems = filteredItems.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedItems = filteredItems;
    }

    function handleItemToggle(e: CustomEvent<{ value: string, enabled: boolean }>) {
        if (e.detail.enabled) {
            cSetting.value = [...cSetting.value, e.detail.value];
        } else {
            cSetting.value = cSetting.value.filter(b => b !== e.detail.value);
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
                <VirtualList items={renderedItems} let:item>
                    <ListItem value={item.value} name={item.name} icon={item.icon}
                            enabled={cSetting.value.includes(item.value)} on:toggle={handleItemToggle}/>
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
      font-weight: 600;
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
