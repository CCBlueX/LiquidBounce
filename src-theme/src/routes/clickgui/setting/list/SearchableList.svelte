<script lang="ts">
    import VirtualList from "./VirtualList.svelte";
    import type {NamedItem} from "../../../../integration/types.ts";

    export let items: NamedItem[];

    let searchQuery = "";
    let renderedItems: NamedItem[] = items;

    $: {
        let filteredItems = items;
        if (searchQuery) {
            filteredItems = filteredItems.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedItems = filteredItems;
    }
</script>

<div class="list-item-list">
    <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery} spellcheck="false">
    <div class="results">
        <VirtualList items={renderedItems} let:item>
            <slot item={item} />
        </VirtualList>
    </div>
</div>

<style lang="scss">
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
    border-bottom: solid 1px var(--accent-color);
    font-family: "Inter", sans-serif;
    font-size: 12px;
    padding: 5px;
    color: var(--clickgui-text-color);
    margin-bottom: 5px;
    background-color: var(--clickgui-input-background-color);
  }
</style>
