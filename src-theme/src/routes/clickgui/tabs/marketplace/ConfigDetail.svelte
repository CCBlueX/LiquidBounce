<script lang="ts">
    import {onMount} from "svelte";
    import DetailHead from "./DetailHead.svelte";
    import Button from "./Button.svelte";
    import Menu from "./Menu.svelte";
    import Tag from "./Tag.svelte";
    import Label from "./Label.svelte";
    import Message from "./Message.svelte";
    import Row from "./Row.svelte";
    import Stat from "./Stat.svelte";
    import Address from "./Address.svelte";
    import EditDetailsModal from "./EditDetailsModal.svelte";
    import DeleteModal from "./DeleteModal.svelte";
    import {copyMarketplaceShareCode, getMarketplaceConfig, reportMarketplaceConfig} from "../../../../integration/rest";
    import type {
        ConfigTracker,
        MarketplaceConfigDetail,
        MarketplaceLinkedConfig
    } from "../../../../integration/types";
    import {
        ago,
        attempt,
        count,
        date,
        message,
        notify,
        reports,
        typeName,
        UNKNOWN_PACK,
        UNKNOWN_SERVER,
        version
    } from "./marketplace";

    let {id, loggedIn, tracker, tags, onback, onload, onupdate, onopen}: {
        id: number;
        loggedIn: boolean;
        tracker: ConfigTracker | null;
        tags: string[];
        onback: () => void;
        onload: (config: MarketplaceLinkedConfig) => void;
        onupdate: () => void;
        onopen: (kind: "config" | "item", id: number) => void;
    } = $props();

    let detail = $state<MarketplaceConfigDetail | null>(null);
    let error = $state<string | null>(null);
    let editOpen = $state(false);
    let deleteOpen = $state(false);

    const config = $derived(detail?.config);
    const tracking = $derived(config && tracker?.id === config.id ? tracker.state : "None");
    const canUpdate = $derived(loggedIn && tracking === "Editing" && !!tracker?.own);
    const configs = $derived(detail?.dependencies.filter(d => !d.status) ?? []);
    const installs = $derived(detail?.dependencies.filter(d => !!d.status) ?? []);
    const owner = $derived(!!config?.own && loggedIn);
    const composed = $derived(configs.length > 0 || installs.length > 0 || !!detail?.changes?.length);

    onMount(refresh);

    async function refresh() {
        error = null;
        try {
            detail = await getMarketplaceConfig(id);
        } catch (e) {
            error = message(e);
        }
    }

    async function report(works: boolean) {
        if (!detail) {
            return;
        }

        const next = detail.report === works ? null : works;
        if (await attempt(() => reportMarketplaceConfig(id, next))) {
            await refresh();
        }
    }

    async function copyShareCode() {
        const code = await attempt(() => copyMarketplaceShareCode(id));
        if (code) {
            notify(`Copied ${code}`);
        }
    }

    function load() {
        if (config) {
            onload({id: config.id, address: config.address});
        }
    }
</script>

{#if detail && config}
    <EditDetailsModal bind:open={editOpen} {detail} {tags} onsaved={refresh}/>
    <DeleteModal bind:open={deleteOpen} {config} ondeleted={onback}/>
{/if}

<div class="detail">
    {#if detail && config}
        <DetailHead image={config.image ?? UNKNOWN_SERVER} {onback}>
            {#snippet title()}<Address address={config.address}/>{/snippet}

            {#snippet tags()}
                {#if tracking !== "None"}
                    <Tag text={tracking === "Editing" ? "Edited" : "Tracked"}/>
                {/if}
                {#if config.featured}
                    <Tag text="Featured"/>
                {/if}
                {#if config.own}
                    <Tag text="Yours"/>
                {/if}
                {#if config.visibility === "unlisted"}
                    <Tag text="Unlisted"/>
                {/if}
                {#if config.overlayOn}
                    <Tag text="Overlay"/>
                {:else if detail?.forkOf}
                    <Tag text="Fork"/>
                {/if}
                {#if config.binds}
                    <Tag text="Binds"/>
                {/if}
            {/snippet}

            {#snippet subtitle()}
                Updated {ago(config.updatedAt)}{detail?.createdAt ? ` · published ${date(detail.createdAt)}` : ""}
            {/snippet}

            {#snippet actions()}
                {#if detail?.shareCode && loggedIn}
                    <Button title={detail.shareCode} mono onclick={copyShareCode}/>
                {/if}
                {#if loggedIn}
                    <Button title="Works · {config.works}" active={detail?.report === true} onclick={() => report(true)}/>
                    <Button title="Broken · {config.fails}" active={detail?.report === false}
                            onclick={() => report(false)}/>
                {/if}
                {#if canUpdate}
                    <Button title="Update..." onclick={onupdate}/>
                {/if}
                <Button title="Load" primary onclick={load}/>
                {#if owner}
                    <Menu entries={[
                        {title: "Edit details", onclick: () => editOpen = true},
                        {title: "Delete config", danger: true, onclick: () => deleteOpen = true}
                    ]}>
                        {#snippet trigger()}More{/snippet}
                    </Menu>
                {/if}
            {/snippet}
        </DetailHead>

        <div class="body">
            <div class="stats">
                <Stat value={ago(config.updatedAt)} label="Last revision"/>
                {#if config.protocol}
                    <Stat value={config.protocol} label="Made on"/>
                {/if}
                <Stat value={reports(config.works, config.fails)} label="Reports"/>
                <Stat value={count(config.downloads)} label="Downloads"/>
                <Stat value={config.visibility === "unlisted" ? "Unlisted" : "Public"} label="Visibility"/>
                <Stat value={config.servers.join(", ") || "Any server"} label="Servers"/>
                {#if detail.forkOf}
                    <Stat label="Fork of">
                        <button type="button" class="link" onclick={() => onopen("config", detail!!.forkOf!!.id)}>
                            {detail.forkOf.address}
                        </button>
                    </Stat>
                {/if}
            </div>

            {#if detail.description || detail.summary}
                <p class="description">{detail.description || detail.summary}</p>
            {/if}

            <div class="columns" class:single={!composed}>
                <div class="column">
                    {#if configs.length > 0}
                        <Label text="Depends on"/>
                        <div class="list">
                            {#each configs as dependency (dependency.id)}
                                <Row image={dependency.image ?? UNKNOWN_SERVER}
                                     onclick={() => onopen("config", dependency.id)}>
                                    {#snippet title()}<Address address={dependency.address}/>{/snippet}
                                    {#snippet tags()}
                                        {#if dependency.featured}
                                            <Tag text="Featured"/>
                                        {/if}
                                    {/snippet}
                                </Row>
                            {/each}
                        </div>
                    {/if}

                    {#if installs.length > 0}
                        <Label text="Installs"/>
                        <div class="list">
                            {#each installs as dependency (dependency.id)}
                                {@const status = dependency.status!!}
                                <Row image={dependency.image ?? UNKNOWN_PACK} dim={!!status.notFor}
                                     onclick={() => onopen("item", dependency.id)}>
                                    {#snippet title()}{dependency.address}{/snippet}
                                    {#snippet tags()}
                                        <Tag text={typeName(dependency.type)}/>
                                        {#if status.subscribed && status.installed}
                                            <Tag text={version(status.installed)}/>
                                        {/if}
                                        {#if status.restartRequired}
                                            <Tag text="Restart needed"/>
                                        {/if}
                                    {/snippet}
                                    {#snippet subtitle()}
                                        {status.notFor ? `Not for ${status.notFor}` : status.summary}
                                    {/snippet}
                                </Row>
                            {/each}
                        </div>
                    {/if}

                    {#if detail.changes && detail.changes.length > 0}
                        <Label text="What it changes"/>
                        <div class="chips">
                            {#each detail.changes as module (module)}
                                <Tag text={module}/>
                            {/each}
                        </div>
                    {/if}
                </div>

                <div class="column">
                    <Label text="History"/>
                    <div class="list">
                        {#each detail.revisions as revision (revision.id)}
                            <Row active={revision.latest}>
                                {#snippet title()}{date(revision.createdAt)}{/snippet}
                                {#snippet tags()}
                                    {#if revision.latest}
                                        <Tag text="Latest"/>
                                    {/if}
                                    {#if revision.loaded}
                                        <Tag text="Loaded"/>
                                    {/if}
                                {/snippet}
                                {#snippet subtitle()}
                                    {revision.changelog ?? (revision.first ? "First version" : "")}
                                {/snippet}
                                {#snippet meta()}{reports(revision.works, revision.fails)}{/snippet}
                            </Row>
                        {/each}
                    </div>
                </div>
            </div>
        </div>
    {:else if error}
        <DetailHead {onback}>
            {#snippet title()}Config{/snippet}
        </DetailHead>
        <Message title="Couldn't open this config">
            {error}
            {#snippet actions()}
                <Button title="Retry" primary onclick={refresh}/>
            {/snippet}
        </Message>
    {/if}
</div>

<style lang="scss">
  .detail {
    flex: 0 1 auto;
    min-height: 0;
    display: flex;
    flex-direction: column;
    background-color: var(--clickgui-window-background-color);
    border-radius: 5px;
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);
    overflow: hidden;
  }

  .body {
    flex: 0 1 auto;
    min-height: 0;
    overflow: auto;
    padding: 14px 20px 20px;

    &::-webkit-scrollbar {
      width: 2px;
    }
  }

  .stats {
    display: flex;
    flex-wrap: wrap;
    gap: 10px 32px;
    padding: 12px 0 4px;
  }

  .description {
    font-size: 12px;
    font-weight: 500;
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
    white-space: pre-wrap;
    margin: 12px 0 0;
    max-width: 820px;
  }

  .columns {
    display: grid;
    grid-template-columns: 1fr 1fr;
    column-gap: 24px;

    &.single {
      grid-template-columns: 1fr;
    }
  }

  .column {
    min-width: 0;
  }

  .list {
    border-radius: 5px;
    overflow: hidden;
    background-color: var(--clickgui-module-settings-background-color);
  }

  .chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .link {
    all: unset;
    cursor: pointer;
    color: var(--accent-color);

    &:hover {
      text-decoration: underline;
    }
  }
</style>
