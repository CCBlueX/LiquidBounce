<script lang="ts">
    import {untrack} from "svelte";
    import Chip from "../../../common/Chip.svelte";
    import ItemActions from "../ItemActions.svelte";
    import DetailPage, {type Fact} from "./DetailPage.svelte";
    import RevisionEntry from "./RevisionEntry.svelte";
    import RevisionList from "./RevisionList.svelte";
    import Section from "./Section.svelte";
    import {getMarketplaceItem} from "../../../../../integration/rest";
    import type {MarketplaceItemDetail} from "../../../../../integration/types";
    import {formatCount, formatDate} from "../../../../../util/utils";
    import {formatVersion, TYPE_NAMES, UNKNOWN_PACK_ICON} from "../marketplace";

    /**
     * [onchange] runs after the item was installed, updated, applied or removed.
     */
    let {id, onback, onchange}: {
        id: number;
        onback: () => void;
        onchange: () => void;
    } = $props();

    let detail = $state<MarketplaceItemDetail | null>(null);
    let error = $state<string | null>(null);

    const item = $derived(detail?.item);
    const installed = $derived(item?.subscribed ? item.installed : undefined);
    const facts = $derived.by((): Fact[] => {
        if (!detail || !item) {
            return [];
        }

        const newest = detail.versions[0]?.revision;
        return [
            {label: "Type", value: TYPE_NAMES[item.type]},
            {label: "Installed", value: installed && formatVersion(installed)},
            {label: "Newest release", value: newest && formatVersion(newest)},
            {label: "Downloads", value: formatCount(item.downloads)},
            {label: "Rating", value: item.rating === undefined ? undefined : `${item.rating.toFixed(1)} of 5`},
            {label: "Reviews", value: item.reviews === 0 ? undefined : item.reviews.toString()}
        ];
    });

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

<DetailPage {id} noun="item" {error} onretry={load} {onback} name={item?.name}
            address={item ? (item.author ? `${item.author}/${item.name}` : item.name) : undefined}
            image={item?.image} fallbackImage={UNKNOWN_PACK_ICON} description={detail?.description} {facts}>
    {#snippet badges()}
        {#if item?.inUse}<Chip active>In use</Chip>{/if}
        {#if item?.subscribed}<Chip active>Installed</Chip>{/if}
        {#if item?.restartRequired}<Chip>Restart needed</Chip>{/if}
        {#if item?.featured}<Chip>Featured</Chip>{/if}
    {/snippet}
    {#snippet subtitle()}
        {#if item?.notFor}Not for {item.notFor}{/if}
    {/snippet}
    {#snippet actions()}
        {#if item}
            <ItemActions {item} removable onchange={() => { onchange(); load(); }}/>
        {/if}
    {/snippet}

    {#if detail}
        <Section title="Versions">
            <RevisionList>
                {#each detail.versions as version (version.revision.id)}
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
                {/each}
            </RevisionList>
        </Section>
    {/if}
</DetailPage>

<style lang="scss">
  .date {
    margin-left: 12px;
  }
</style>
