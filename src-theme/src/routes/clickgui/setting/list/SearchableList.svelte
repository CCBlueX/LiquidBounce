<script lang="ts">
    import VirtualList from "./VirtualList.svelte";
    import type {NamedItem} from "../../../../integration/types.ts";
    import {escapeLayer} from "../../../../integration/escape";

    export let items: NamedItem[];
    /**
     * The items matching the search, for whoever wants to act on all of them.
     */
    export let filtered: NamedItem[] = items;

    const ROW_HEIGHT = 31;
    const MAX_HEIGHT = 200;

    let searchQuery = "";

    $: {
        const queryParts = searchQuery.toLowerCase().match(/\S+/g) ?? [];

        filtered = items.filter(item => {
            const name = item.name.toLowerCase();
            return queryParts.every(part => name.includes(part));
        });
    }

    const layer = escapeLayer(() => searchQuery = "");
    $: layer.setOpen(searchQuery !== "");
</script>

<div class="list-item-list">
    <input type="text" placeholder="Search" aria-label="Search" class="search-input" bind:value={searchQuery}
           spellcheck="false">
    {#if filtered.length === 0}
        <div class="empty">Nothing found</div>
    {:else}
        <div class="results" style="height: {Math.min(filtered.length * ROW_HEIGHT, MAX_HEIGHT)}px">
            <VirtualList items={filtered} let:item>
                <slot item={item} />
            </VirtualList>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../common/underlined-input" as *;

  .list-item-list {
    padding-bottom: 5px;
  }

  .results {
    position: relative;
    overflow: hidden;
  }

  .search-input {
    @include underlined-input;
    width: 100%;
    margin-bottom: 6px;
  }

  .empty {
    height: 28px;
    line-height: 28px;
    font-size: 11px;
    color: var(--clickgui-text-muted-color);
  }
</style>
