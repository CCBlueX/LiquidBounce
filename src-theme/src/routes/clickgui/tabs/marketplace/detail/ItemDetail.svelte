<script lang="ts">
    import {untrack} from "svelte";
    import Chip from "../../../common/Chip.svelte";
    import ItemActions from "../ItemActions.svelte";
    import DetailPage from "./DetailPage.svelte";
    import RevisionEntry from "./RevisionEntry.svelte";
    import RevisionList from "./RevisionList.svelte";
    import Section from "./Section.svelte";
    import {getMarketplaceItem} from "../../../../../integration/rest";
    import type {MarketplaceItemDetail} from "../../../../../integration/types";
    import {formatCount, formatDate} from "../../../../../util/utils";
    import {formatVersion, UNKNOWN_PACK_ICON} from "../marketplace";

    /**
     * [onchange] runs after the item was installed, updated, applied or removed.
     */
    let {id, onchange}: {
        id: number;
        onchange: () => void;
    } = $props();

    let detail = $state<MarketplaceItemDetail | null>(null);
    let error = $state<string | null>(null);

    const item = $derived(detail?.item);
    const installed = $derived(item?.subscribed ? item.installed : undefined);

    $effect(() => {
        void id;
        untrack(load);
    });

    async function load() {
        try {
            detail = await getMarketplaceItem(id);
            error = null;
        } catch (e) {
            error = e instanceof Error ? e.message : String(e);
        }
    }
</script>

<DetailPage {id} noun="item" {error} onretry={load} name={item?.name}
            address={item ? (item.author ? `${item.author}/${item.name}` : item.name) : undefined}
            image={item?.image} fallbackImage={UNKNOWN_PACK_ICON} description={detail?.description}>
    {#snippet badges()}
        {#if item?.inUse}<Chip active>In use</Chip>{/if}
        {#if installed}<Chip active>{formatVersion(installed)}</Chip>{/if}
        {#if item?.restartRequired}<Chip>Restart needed</Chip>{/if}
        {#if item?.featured}<Chip>Featured</Chip>{/if}
    {/snippet}
    {#snippet subtitle()}
        {#if item}
            {#if item.notFor}Not for {item.notFor} ·{/if}
            {#if item.rating !== undefined}
                <span class="rating">{item.rating.toFixed(1)}</span> ·
            {/if}
            {formatCount(item.downloads)} downloads
        {/if}
    {/snippet}
    {#snippet actions()}
        {#if item}
            <ItemActions {item} removable onchange={() => { onchange(); load(); }}/>
        {/if}
    {/snippet}

    {#if detail && detail.versions.length > 0}
        <Section title="Changelog">
            <RevisionList items={detail.versions} noun="version">
                {#snippet entry(version)}
                    {@const revision = version.revision}
                    <RevisionEntry title={formatVersion(revision)} text={revision.changelog}
                                   highlighted={version.installed} dimmed={!version.fits && !version.installed}>
                        {#snippet badges()}
                            {#if version.installed}<Chip active>Installed</Chip>{/if}
                        {/snippet}
                        {#snippet meta()}
                            {#if !version.fits}
                                Not for {detail?.liquidbounce}
                            {:else if revision.liquidbounce}
                                For {revision.liquidbounce}
                            {/if}
                            {#if revision.createdAt !== undefined}
                                <span class="date">{formatDate(revision.createdAt)}</span>
                            {/if}
                        {/snippet}
                    </RevisionEntry>
                {/snippet}
            </RevisionList>
        </Section>
    {/if}
</DetailPage>

<style lang="scss">
  .rating {
    padding-left: 15px;
    background: url("/img/menu/icon-favorite-filled.svg") left center / 11px no-repeat;
  }

  .date {
    margin-left: 12px;
  }
</style>
