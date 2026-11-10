<script lang="ts" generics="T extends { id: number }, P extends MarketplacePage<T>">
    import type {Snippet} from "svelte";
    import type {MarketplacePage} from "../../../../../integration/types";
    import Button from "../../../common/Button.svelte";
    import Placeholder from "../Placeholder.svelte";
    import type {PagedList} from "./paged_list.svelte";

    /**
     * The scrolling rows of [list], which loads its next page when the end comes into view. [offline] shows
     * below the notice that the marketplace can not be reached.
     */
    let {list, noun, entry, empty, offline}: {
        list: PagedList<T, P>;
        noun: string;
        entry: Snippet<[T]>;
        empty: Snippet;
        offline?: Snippet;
    } = $props();

    let end: HTMLElement;

    $effect(() => {
        // Observing again after every page reports an end that is still in view
        void list.items;
        const observer = new IntersectionObserver(entries => {
            if (entries.some(entry => entry.isIntersecting)) {
                list.loadMore();
            }
        });
        observer.observe(end);

        return () => observer.disconnect();
    });
</script>

<div class="list">
    {#if list.offline}
        <Placeholder title="Can't reach the marketplace">
            {#snippet actions()}
                <Button onclick={() => list.reload()}>Retry</Button>
            {/snippet}
        </Placeholder>
        {@render offline?.()}
    {:else if list.error}
        <Placeholder title="Couldn't load {noun}" text={list.error}>
            {#snippet actions()}
                <Button onclick={() => list.reload()}>Retry</Button>
            {/snippet}
        </Placeholder>
    {:else}
        {#each list.items as item (item.id)}
            {@render entry(item)}
        {/each}
        {#if list.items.length === 0 && !list.loading}
            {@render empty()}
        {/if}
    {/if}
    <div bind:this={end}></div>
</div>

<style lang="scss">
  .list {
    flex: 1;
    display: flex;
    flex-direction: column;
    row-gap: 6px;
    min-height: 0;
    padding: 0 14px 14px;
    overflow-y: auto;
    scrollbar-gutter: stable;

    &::-webkit-scrollbar {
      width: 4px;
    }
  }
</style>
