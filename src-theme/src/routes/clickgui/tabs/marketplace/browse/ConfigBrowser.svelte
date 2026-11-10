<script lang="ts">
    import {onMount, untrack} from "svelte";
    import Button from "../../../common/Button.svelte";
    import Chip from "../../../common/Chip.svelte";
    import Placeholder from "../Placeholder.svelte";
    import Reports from "../Reports.svelte";
    import Entry from "./Entry.svelte";
    import EntryList from "./EntryList.svelte";
    import SearchInput from "./SearchInput.svelte";
    import TrackerEntry from "./TrackerEntry.svelte";
    import {getMarketplaceConfigs, getModule, getModuleSettings} from "../../../../../integration/rest";
    import type {MarketplaceConfig, MarketplaceConfigPage} from "../../../../../integration/types";
    import {formatCount} from "../../../../../util/utils";
    import {
        type ConfigFilters,
        currentServer,
        dialog,
        formatConfigSubtitle,
        getTrackingState,
        SORTS,
        tracker,
        UNKNOWN_SERVER_ICON,
        user
    } from "../marketplace";
    import {PagedList} from "./paged_list.svelte";

    let {filters = $bindable(), onopen}: {
        filters: ConfigFilters;
        onopen: (id: number) => void;
    } = $props();

    let query = $state("");
    let autoConfigOnlyFeatured = $state(false);

    const server = $derived(filters.onServer ? $currentServer : null);
    const request = $derived({
        query,
        tags: filters.tags,
        server: server !== null,
        featured: filters.featured,
        sort: SORTS[filters.sort]
    });
    const filtered = $derived(query !== "" || filters.featured || server !== null || filters.tags.length > 0);

    const list = new PagedList<MarketplaceConfig, MarketplaceConfigPage>(
        page => getMarketplaceConfigs({...request, page})
    );
    const hidden = $derived(list.lastPage?.unfeatured ?? 0);

    $effect(() => {
        // What a config says about the account changes with it
        void [request, $user];
        untrack(() => list.reload());
    });

    onMount(async () => {
        // With OnlyFeatured, AutoConfig loads nothing on a server that has no featured config
        const [module, settings] = await Promise.all([
            getModule("AutoConfig"),
            getModuleSettings("AutoConfig")
        ]).catch(() => []);
        autoConfigOnlyFeatured = !!module?.enabled
            && !!settings?.value.some(setting => setting.name === "OnlyFeatured" && setting.value === true);
    });

    function clearFilters() {
        query = "";
        filters = {...filters, featured: false, onServer: false, tags: []};
    }
</script>

<div class="top">
    <SearchInput bind:query placeholder="Search or paste a share code"/>
    <TrackerEntry {onopen}/>
</div>

<EntryList {list} noun="the configs">
    {#snippet entry(config: MarketplaceConfig)}
        {@const tracking = getTrackingState(config.id, $tracker)}
        <Entry address={config.address} image={config.image} fallbackImage={UNKNOWN_SERVER_ICON}
               highlighted={tracking !== "None"} onopen={() => onopen(config.id)}>
            {#snippet badges()}
                {#if tracking !== "None"}
                    <Chip active>{tracking === "Editing" ? "Edited" : "Tracked"}</Chip>
                {/if}
                {#if config.featured}<Chip>Featured</Chip>{/if}
                {#if config.own}<Chip>Yours</Chip>{/if}
                {#if config.visibility === "unlisted"}<Chip>Unlisted</Chip>{/if}
                {#if config.overlayOn}<Chip>Overlay on {config.overlayOn}</Chip>{/if}
                {#if config.binds}<Chip>Binds</Chip>{/if}
                {#each config.tags as tag (tag)}
                    <Chip>{tag}</Chip>
                {/each}
            {/snippet}
            {#snippet subtitle()}{formatConfigSubtitle(config)}{/snippet}
            {#snippet meta()}
                <Reports works={config.works} fails={config.fails}/>
                <span class="downloads">{formatCount(config.downloads)} downloads</span>
            {/snippet}
            {#snippet actions()}
                <Button onclick={() => dialog.set({kind: "load", config})}>Load</Button>
            {/snippet}
        </Entry>
    {/snippet}

    {#snippet empty()}
        {#if list.lastPage?.code}
            <Placeholder title="No config has this share code">
                {#snippet actions()}
                    <Button variant="secondary" onclick={clearFilters}>Clear search</Button>
                {/snippet}
            </Placeholder>
        {:else if filters.featured && hidden > 0}
            <Placeholder title="No featured config{server ? ` for ${server}` : ''}"
                         text="{hidden} {hidden === 1 ? 'config matches' : 'configs match'}, none featured yet.{
                             server && autoConfigOnlyFeatured
                                 ? ' AutoConfig loads nothing here while its OnlyFeatured setting is on.' : ''}">
                {#snippet actions()}
                    <Button onclick={() => filters = {...filters, featured: false}}>
                        Show {hidden === 1 ? "it" : `${hidden} configs`}
                    </Button>
                    <Button variant="secondary" onclick={clearFilters}>Clear filters</Button>
                {/snippet}
            </Placeholder>
        {:else}
            <Placeholder title="No config found">
                {#snippet actions()}
                    {#if filtered}
                        <Button variant="secondary" onclick={clearFilters}>Clear filters</Button>
                    {/if}
                {/snippet}
            </Placeholder>
        {/if}
    {/snippet}
</EntryList>

<style lang="scss">
  .top {
    display: flex;
    flex-direction: column;
    row-gap: 10px;
    padding: 14px 14px 10px;
  }

  .downloads {
    min-width: 100px;
    text-align: right;
  }
</style>
