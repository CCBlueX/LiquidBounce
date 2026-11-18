<script lang="ts">
    import VirtualList from "./VirtualList.svelte";
    import type {NamedItem} from "../../../../integration/types.ts";
    import {escapeLayer} from "../../../../integration/escape";

    export let items: NamedItem[];

    let searchQuery = "";
    let renderedItems: NamedItem[] = items;

    $: {
        const queryParts = searchQuery.toLowerCase().match(/\S+/g) ?? [];

        renderedItems = items.filter(item => {
            const name = item.name.toLowerCase();
            return queryParts.every(part => name.includes(part));
        });
    }

    const layer = escapeLayer(() => searchQuery = "");
    $: layer.setOpen(searchQuery !== "");
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
    border: 1px solid var(--accent-border-color);
    border-radius: var(--radius-xs);
    font-family: "Inter", sans-serif;
    font-size: 12px;
    padding: 7px;
    color: var(--clickgui-text-color);
    margin-bottom: 7px;
    background-color: var(--clickgui-input-background-color);
    transition: border-color var(--duration-fast) var(--ease-standard),
    box-shadow var(--duration-fast) var(--ease-standard);

    &:focus-visible {
      border-color: var(--accent-color);
      box-shadow: 0 0 0 2px var(--accent-subtle-background-color);
    }
  }
</style>
