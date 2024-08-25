<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {ItemsSetting, ModuleSetting} from "../../../../integration/types";
    import {getRegistries} from "../../../../integration/rest";
    import Item from "./Item.svelte";
    import VirtualList from "./VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as ItemsSetting;

    interface Item {
        name: string;
        identifier: string;
    }

    const dispatch = createEventDispatcher();
    let items: Item[] = [];
    let renderedItems: Item[] = items;
    let searchQuery = "";

    $: {
        let filteredItems = items;
        if (searchQuery) {
            filteredItems = filteredItems.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedItems = filteredItems;
    }

    onMount(async () => {
        let i = (await getRegistries()).items;

        if (i !== undefined) {
            items = i.sort((a, b) => a.identifier.localeCompare(b.identifier));
        }
    });

    function handleItemToggle(e: CustomEvent<{ identifier: string, enabled: boolean }>) {
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
    <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery} spellcheck="false">
    <div class="results">
        <VirtualList items={renderedItems} let:item>
            <Item identifier={item.identifier} name={item.name} enabled={cSetting.value.includes(item.identifier)} on:toggle={handleItemToggle}/>
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
