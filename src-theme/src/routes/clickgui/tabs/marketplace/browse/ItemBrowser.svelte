<script lang="ts">
    import {untrack} from "svelte";
    import Chip from "../../../common/Chip.svelte";
    import Markdown from "../../../common/Markdown.svelte";
    import ItemActions from "../ItemActions.svelte";
    import Placeholder from "../Placeholder.svelte";
    import Entry from "./Entry.svelte";
    import EntryList from "./EntryList.svelte";
    import FilterBar from "./FilterBar.svelte";
    import {getInstalledMarketplaceItems, getMarketplaceItems} from "../../../../../integration/rest";
    import type {
        MarketplaceInstalledItem,
        MarketplaceItem
    } from "../../../../../integration/types";
    import {parseMarkdownSummary} from "../../../../../util/markdown";
    import {formatCount} from "../../../../../util/utils";
    import {formatVersion, type ListedType, SORTS, TYPE_TITLES, UNKNOWN_PACK_ICON} from "../marketplace";
    import {PagedList} from "./paged_list.svelte";

    /**
     * [changes] counts the installs and removals made elsewhere, which the list reloads for.
     */
    let {type, sort = $bindable(), changes, onopen}: {
        type: ListedType;
        sort: string;
        changes: number;
        onopen: (id: number) => void;
    } = $props();

    let query = $state("");
    let installed = $state<MarketplaceInstalledItem[]>([]);

    const title = $derived(TYPE_TITLES[type].toLowerCase());
    const list = new PagedList<MarketplaceItem>(page => getMarketplaceItems(type, page, query, SORTS[sort]));

    $effect(() => {
        void [query, sort, changes];
        untrack(() => list.reload());
    });

    $effect(() => {
        // What is installed is known without the marketplace
        if (list.offline) {
            getInstalledMarketplaceItems().then(items => installed = items, () => installed = []);
        }
    });
</script>

<div class="top">
    <FilterBar bind:query bind:sort placeholder="Search {title}"/>
</div>

<EntryList {list} noun="the {title}">
    {#snippet entry(item: MarketplaceItem)}
        {@const summary = parseMarkdownSummary(item.summary, item.name)}
        <Entry address={item.author ? `${item.author}/${item.name}` : item.name} image={item.image}
               fallbackImage={UNKNOWN_PACK_ICON} dimmed={!!item.notFor} onopen={() => onopen(item.id)}>
            {#snippet badges()}
                {#if item.inUse}<Chip active>In use</Chip>{/if}
                {#if item.subscribed && item.installed}<Chip active>{formatVersion(item.installed)}</Chip>{/if}
                {#if item.restartRequired}<Chip>Restart needed</Chip>{/if}
                {#if item.featured}<Chip>Featured</Chip>{/if}
            {/snippet}
            {#snippet subtitle()}
                <Markdown tokens={summary} inline/>
            {/snippet}
            {#snippet meta()}
                {#if item.notFor}
                    <span>Not for {item.notFor}</span>
                {/if}
                {#if item.rating !== undefined}
                    <span class="rating">{item.rating.toFixed(1)}</span>
                {/if}
                <span>{formatCount(item.downloads)} downloads</span>
            {/snippet}
            {#snippet actions()}
                <ItemActions {item} onchange={() => list.reload()}/>
            {/snippet}
        </Entry>
    {/snippet}

    {#snippet empty()}
        <Placeholder title="Nothing found"/>
    {/snippet}

    {#snippet offline()}
        {#each installed.filter(item => item.type === type) as item (item.id)}
            <Entry address={item.name} fallbackImage={UNKNOWN_PACK_ICON}>
                {#snippet badges()}
                    <Chip active>Installed</Chip>
                {/snippet}
            </Entry>
        {/each}
    {/snippet}
</EntryList>

<style lang="scss">
  .top {
    padding: 14px 14px 10px;
  }

  .rating {
    display: inline-flex;
    align-items: center;
    column-gap: 5px;
    color: var(--clickgui-marketplace-rating-color);

    &::before {
      content: "";
      width: 11px;
      height: 11px;
      background-color: currentColor;
      mask: url("/img/menu/icon-favorite-filled.svg") center / contain no-repeat;
    }
  }
</style>
