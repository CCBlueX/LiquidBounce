<script lang="ts">
    import {untrack} from "svelte";
    import Button from "../../../common/Button.svelte";
    import Chip from "../../../common/Chip.svelte";
    import Markdown from "../../../common/Markdown.svelte";
    import Entry from "../browse/Entry.svelte";
    import Reports from "../Reports.svelte";
    import DetailPage, {type Fact} from "./DetailPage.svelte";
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
    import {formatCount, formatDate, formatTimeAgo} from "../../../../../util/utils";
    import {
        dialog,
        formatVersion,
        getTrackingState,
        tracker,
        TYPE_NAMES,
        UNKNOWN_PACK_ICON,
        UNKNOWN_SERVER_ICON,
        user,
        type View
    } from "../marketplace";

    let {id, onback, onopen}: {
        id: number;
        onback: () => void;
        onopen: (view: View) => void;
    } = $props();

    let detail = $state<MarketplaceConfigDetail | null>(null);
    let error = $state<string | null>(null);

    const config = $derived(detail?.config);
    const tracking = $derived(getTrackingState(id, $tracker));
    const canUpdate = $derived(!!$user && tracking === "Editing" && !!$tracker?.editable);
    const facts = $derived.by((): Fact[] => {
        if (!detail || !config) {
            return [];
        }

        const forkOf = detail.forkOf;
        return [
            {label: "Downloads", value: formatCount(config.downloads)},
            {label: "Made on", value: config.protocol},
            {label: "Servers", value: config.servers.join(", ") || "Any server"},
            {label: "Visibility", value: config.visibility === "unlisted" ? "Unlisted" : "Public"},
            {label: "Published", value: detail.createdAt === undefined ? undefined : formatDate(detail.createdAt)},
            {label: "Fork of", value: forkOf?.address, onclick: () => forkOf && onopen({kind: "config", id: forkOf.id})}
        ];
    });

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

<DetailPage noun="config" {error} onretry={load} {onback} address={config?.address} name={config?.name}
            image={config?.image} fallbackImage={UNKNOWN_SERVER_ICON} description={detail?.description} {facts}>
    {#snippet badges()}
        {#if tracking !== "None"}
            <Chip active>{tracking === "Editing" ? "Edited" : "Tracked"}</Chip>
        {/if}
        {#if config?.featured}<Chip>Featured</Chip>{/if}
        {#if config?.own}<Chip>Yours</Chip>{/if}
        {#if config?.overlayOn}<Chip>Overlay</Chip>{:else if detail?.forkOf}<Chip>Fork</Chip>{/if}
        {#if config?.binds}<Chip>Binds</Chip>{/if}
        {#each config?.tags ?? [] as tag (tag)}
            <Chip>{tag}</Chip>
        {/each}
    {/snippet}
    {#snippet subtitle()}
        {#if config?.updatedAt !== undefined}Updated {formatTimeAgo(config.updatedAt)}{/if}
    {/snippet}
    {#snippet actions()}
        {#if config && $user}
            <Button variant="secondary" pressed={detail?.report === true} onclick={() => report(true)}>
                Works · {config.works}
            </Button>
            <Button variant="secondary" pressed={detail?.report === false} onclick={() => report(false)}>
                Broken · {config.fails}
            </Button>
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
                        <Button variant="danger" onclick={() => $dialog = {kind: "delete", config, ondone: onback}}>
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
                                    <Markdown tokens={parseMarkdownSummary(item.summary, item.name)} inline
                                              links={false}/>
                                {/if}
                            {/snippet}
                        </Entry>
                    {/each}
                </div>
            </Section>
        {/if}

        {#if detail.changes && detail.changes.length > 0}
            <Section title="Changes">
                <div class="chips">
                    {#each detail.changes as module (module)}
                        <Chip>{module}</Chip>
                    {/each}
                </div>
            </Section>
        {/if}

        <Section title="History">
            <RevisionList>
                {#each detail.revisions as revision (revision.id)}
                    <RevisionEntry title={revision.createdAt === undefined ? "" : formatDate(revision.createdAt)}
                                   text={revision.changelog ?? (revision.first ? "First version" : "")}
                                   highlighted={revision.loaded}>
                        {#snippet badges()}
                            {#if revision.latest}<Chip>Latest</Chip>{/if}
                            {#if revision.loaded}<Chip active>Loaded</Chip>{/if}
                        {/snippet}
                        {#snippet meta()}
                            <Reports works={revision.works} fails={revision.fails}/>
                        {/snippet}
                    </RevisionEntry>
                {/each}
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

  .chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
</style>
