<script lang="ts">
    import {untrack} from "svelte";
    import Button from "../../../common/Button.svelte";
    import Chip from "../../../common/Chip.svelte";
    import Markdown from "../../../common/Markdown.svelte";
    import Entry from "../browse/Entry.svelte";
    import Reports from "../Reports.svelte";
    import DetailPage from "./DetailPage.svelte";
    import RevisionEntry from "./RevisionEntry.svelte";
    import RevisionList from "./RevisionList.svelte";
    import Section from "./Section.svelte";
    import {
        copyMarketplaceShareCode,
        getMarketplaceConfig,
        reportMarketplaceConfig
    } from "../../../../../integration/rest";
    import type {MarketplaceConfigDetail} from "../../../../../integration/types";
    import {parseMarkdownSummary} from "../../../../../util/markdown";
    import {formatCount, formatDate} from "../../../../../util/utils";
    import {
        dialog,
        formatConfigSubtitle,
        formatVersion,
        getTrackingState,
        tracker,
        TYPE_NAMES,
        UNKNOWN_PACK_ICON,
        UNKNOWN_SERVER_ICON,
        user,
        type View
    } from "../marketplace";

    let {id, ondelete, onopen}: {
        id: number;
        ondelete: () => void;
        onopen: (view: View) => void;
    } = $props();

    let detail = $state<MarketplaceConfigDetail | null>(null);
    let error = $state<string | null>(null);

    const config = $derived(detail?.config);
    const tracking = $derived(getTrackingState(id, $tracker));
    const canUpdate = $derived(!!$user && tracking === "Editing" && !!$tracker?.editable);
    const forkOf = $derived(detail?.forkOf);

    $effect(() => {
        // The account decides what it may do here, the tracker which revision counts as loaded
        void [id, $user, $tracker];
        untrack(load);
    });

    async function load() {
        try {
            detail = await getMarketplaceConfig(id);
            error = null;
        } catch (e) {
            error = e instanceof Error ? e.message : String(e);
        }
    }

    async function report(works: boolean) {
        // Reporting the same again takes the report back
        const next = detail?.report === works ? null : works;
        if (await reportMarketplaceConfig(id, next).catch(() => null)) {
            await load();
        }
    }
</script>

<DetailPage {id} noun="config" {error} onretry={load} address={config?.address} name={config?.name}
            image={config?.image} fallbackImage={UNKNOWN_SERVER_ICON} description={detail?.description}>
    {#snippet badges()}
        {#if tracking !== "None"}
            <Chip active>{tracking === "Editing" ? "Edited" : "Tracked"}</Chip>
        {/if}
        {#if config?.featured}<Chip>Featured</Chip>{/if}
        {#if config?.own}<Chip>Yours</Chip>{/if}
        {#if config?.visibility === "unlisted"}<Chip>Unlisted</Chip>{/if}
        {#if config?.overlayOn}<Chip>Overlay</Chip>{/if}
        {#if config?.binds}<Chip>Binds</Chip>{/if}
        {#each config?.tags ?? [] as tag (tag)}
            <Chip>{tag}</Chip>
        {/each}
    {/snippet}
    {#snippet subtitle()}
        {#if config}
            {formatConfigSubtitle(config)} · {formatCount(config.downloads)} downloads
            {#if forkOf}
                · fork of
                <button type="button" class="link" onclick={() => onopen({kind: "config", id: forkOf.id})}>
                    {forkOf.address}
                </button>
            {/if}
        {/if}
    {/snippet}
    {#snippet actions()}
        {#if config && $user}
            <Reports works={config.works} fails={config.fails} own={detail?.report} onreport={report}/>
        {:else if config}
            <span class="reports"><Reports works={config.works} fails={config.fails}/></span>
        {/if}
        {#if config}
            <Button onclick={() => $dialog = {kind: "load", config}}>Load</Button>
        {/if}
    {/snippet}

    {#if detail && config}
        {@const shown = detail}
        {#if $user && (config.own || canUpdate || detail.shareCode)}
            <Section title="Manage">
                <div class="manage">
                    {#if canUpdate}
                        <Button onclick={() => $dialog = {kind: "update"}}>Update...</Button>
                    {/if}
                    {#if config.own}
                        <Button variant="secondary"
                                onclick={() => $dialog = {kind: "edit", detail: shown, ondone: load}}>
                            Edit listing
                        </Button>
                    {/if}
                    {#if detail.shareCode}
                        <Button variant="secondary" onclick={() => copyMarketplaceShareCode(id).catch(() => null)}>
                            Copy share code
                        </Button>
                        <code class="code">{detail.shareCode}</code>
                    {/if}
                    {#if config.own}
                        <span class="spacer"></span>
                        <Button variant="danger" onclick={() => $dialog = {kind: "delete", config, ondone: ondelete}}>
                            Delete
                        </Button>
                    {/if}
                </div>
            </Section>
        {/if}

        {#if detail.configs.length > 0}
            <Section title="Loads on top of">
                <div class="entries">
                    {#each detail.configs as dependency (dependency.id)}
                        <Entry address={dependency.address} image={dependency.image}
                               fallbackImage={UNKNOWN_SERVER_ICON}
                               onopen={() => onopen({kind: "config", id: dependency.id})}>
                            {#snippet badges()}
                                {#if dependency.featured}<Chip>Featured</Chip>{/if}
                            {/snippet}
                        </Entry>
                    {/each}
                </div>
            </Section>
        {/if}

        {#if detail.installs.length > 0}
            <Section title="Installs">
                <div class="entries">
                    {#each detail.installs as item (item.id)}
                        <Entry address={item.author ? `${item.author}/${item.name}` : item.name} image={item.image}
                               fallbackImage={UNKNOWN_PACK_ICON} dimmed={!!item.notFor}
                               onopen={() => onopen({kind: "item", id: item.id})}>
                            {#snippet badges()}
                                <Chip>{TYPE_NAMES[item.type]}</Chip>
                                {#if item.subscribed && item.installed}
                                    <Chip active>{formatVersion(item.installed)}</Chip>
                                {/if}
                                {#if item.restartRequired}<Chip>Restart needed</Chip>{/if}
                            {/snippet}
                            {#snippet subtitle()}
                                {#if item.notFor}
                                    Not for {item.notFor}
                                {:else}
                                    <Markdown tokens={parseMarkdownSummary(item.summary, item.name)} inline/>
                                {/if}
                            {/snippet}
                        </Entry>
                    {/each}
                </div>
            </Section>
        {/if}

        {#if detail.changes && detail.changes.length > 0}
            {@const named = detail.changes.slice(0, 8)}
            <Section title="Changes">
                <p class="changes">
                    {named.join(", ")}{#if detail.changes.length > named.length}
                        and {detail.changes.length - named.length} more{/if}
                </p>
            </Section>
        {/if}

        <Section title="Changelog">
            <RevisionList items={detail.revisions} noun="revision">
                {#snippet entry(revision)}
                    <RevisionEntry title={revision.createdAt === undefined ? "" : formatDate(revision.createdAt)}
                                   text={revision.changelog ?? (revision.first ? "First version" : "")}
                                   highlighted={revision.loaded}>
                        {#snippet badges()}
                            {#if revision.loaded}<Chip active>Loaded</Chip>{/if}
                        {/snippet}
                        {#snippet meta()}
                            <Reports works={revision.works} fails={revision.fails}/>
                        {/snippet}
                    </RevisionEntry>
                {/snippet}
            </RevisionList>
        </Section>
    {/if}
</DetailPage>

<style lang="scss">
  .reports {
    margin-right: 8px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
  }

  .link {
    padding: 0;
    border: none;
    background: none;
    font: inherit;
    color: var(--clickgui-text-color);
    cursor: pointer;

    &:hover, &:focus-visible {
      text-decoration: underline;
    }
  }

  .manage {
    display: flex;
    align-items: center;
    column-gap: 6px;
  }

  .code {
    margin-left: 4px;
    font-family: monospace;
    font-size: 12px;
    letter-spacing: 1px;
    color: var(--clickgui-text-dimmed-color);
    user-select: text;
  }

  .spacer {
    flex: 1;
  }

  .entries {
    display: flex;
    flex-direction: column;
    row-gap: 6px;
  }

  .changes {
    font-size: 12px;
    font-weight: 500;
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
