<script lang="ts">
    import {onMount} from "svelte";
    import DetailPage from "./DetailPage.svelte";
    import PillButton from "../../../common/PillButton.svelte";
    import ActionMenu from "../../../common/ActionMenu.svelte";
    import Chip from "../../../common/Chip.svelte";
    import SectionLabel from "../../../common/SectionLabel.svelte";
    import ListRow from "../../../common/ListRow.svelte";
    import {copyMarketplaceShareCode, getMarketplaceConfig, reportMarketplaceConfig} from "../../../../../integration/rest";
    import type {ConfigTracker, MarketplaceConfigDetail} from "../../../../../integration/types";
    import {
        attempt,
        configBadges,
        dialog,
        reports,
        typeName,
        UNKNOWN_PACK,
        UNKNOWN_SERVER,
        version
    } from "../marketplace";
    import {ago, compactNumber, date, errorMessage, present} from "../../../../../util/utils";

    let {id, loggedIn, tracker, onback, onopen}: {
        id: number;
        loggedIn: boolean;
        tracker: ConfigTracker | null;
        onback: () => void;
        onopen: (kind: "config" | "item", id: number) => void;
    } = $props();

    let detail = $state<MarketplaceConfigDetail | null>(null);
    let error = $state<string | null>(null);

    const config = $derived(detail?.config);
    const tracking = $derived(config && tracker?.id === config.id ? tracker.state : "None");
    const canUpdate = $derived(loggedIn && tracking === "Editing" && !!tracker?.own);
    const configs = $derived(detail?.configs ?? []);
    const installs = $derived(detail?.installs ?? []);
    const owner = $derived(!!config?.own && loggedIn);
    const composed = $derived(configs.length > 0 || installs.length > 0 || !!detail?.changes?.length);
    const stats = $derived(config && detail ? [
        {label: "Last revision", value: ago(config.updatedAt)},
        {label: "Made on", value: config.protocol},
        {label: "Reports", value: reports(config.works, config.fails)},
        {label: "Downloads", value: compactNumber(config.downloads)},
        {label: "Visibility", value: config.visibility === "unlisted" ? "Unlisted" : "Public"},
        {label: "Servers", value: config.servers.join(", ") || "Any server"},
        {label: "Fork of", value: detail.forkOf?.address, onclick: () => detail?.forkOf && onopen("config", detail.forkOf.id)}
    ] : []);

    onMount(refresh);

    async function refresh() {
        error = null;
        try {
            detail = await getMarketplaceConfig(id);
        } catch (e) {
            error = errorMessage(e);
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
        await attempt(() => copyMarketplaceShareCode(id));
    }

    function load() {
        if (config) {
            dialog.set({kind: "load", config: {id: config.id, address: config.address}});
        }
    }
</script>

<DetailPage noun="config" loaded={!!config} {error} onretry={refresh} {onback} {stats}
            image={config?.image ?? UNKNOWN_SERVER} address={config?.address}
            badges={config ? configBadges(config, tracking, config.overlayOn ? "Overlay" : detail?.forkOf && "Fork") : []}
            subtitle="Updated {ago(config?.updatedAt)}{detail?.createdAt ? ` · published ${date(detail.createdAt)}` : ''}"
            description={detail?.description}>
    {#snippet actions()}
        {#if detail?.shareCode && loggedIn}
            <PillButton title={detail.shareCode} mono onclick={copyShareCode}/>
        {/if}
        {#if config && loggedIn}
            <PillButton title="Works · {config.works}" active={detail?.report === true} onclick={() => report(true)}/>
            <PillButton title="Broken · {config.fails}" active={detail?.report === false} onclick={() => report(false)}/>
        {/if}
        {#if canUpdate}
            <PillButton title="Update..." onclick={() => dialog.set({kind: "update"})}/>
        {/if}
        <PillButton title="Load" primary onclick={load}/>
        {#if config && detail && owner}
            <ActionMenu entries={[
                {title: "Edit details", onclick: () => dialog.set({kind: "edit", detail: detail!!, ondone: refresh})},
                {title: "Delete config", danger: true, onclick: () => dialog.set({kind: "delete", config, ondone: onback})}
            ]}>
                {#snippet trigger()}More{/snippet}
            </ActionMenu>
        {/if}
    {/snippet}

    {#if detail}
        <div class="columns" class:single={!composed}>
            <div class="column">
                {#if configs.length > 0}
                    <SectionLabel text="Depends on"/>
                    <div class="list">
                        {#each configs as dependency (dependency.id)}
                            <ListRow image={dependency.image ?? UNKNOWN_SERVER} address={dependency.address}
                                     badges={present(dependency.featured && "Featured")}
                                     onclick={() => onopen("config", dependency.id)}/>
                        {/each}
                    </div>
                {/if}

                {#if installs.length > 0}
                    <SectionLabel text="Installs"/>
                    <div class="list">
                        {#each installs as item (item.id)}
                            <ListRow image={item.image ?? UNKNOWN_PACK} title={item.author ? `${item.author}/${item.name}` : item.name}
                                     badges={present(
                                         typeName(item.type),
                                         item.subscribed && !!item.installed && version(item.installed),
                                         item.restartRequired && "Restart needed"
                                     )}
                                     subtitle={item.notFor ? `Not for ${item.notFor}` : item.summary}
                                     dim={!!item.notFor} onclick={() => onopen("item", item.id)}/>
                        {/each}
                    </div>
                {/if}

                {#if detail.changes && detail.changes.length > 0}
                    <SectionLabel text="What it changes"/>
                    <div class="chips">
                        {#each detail.changes as module (module)}
                            <Chip text={module}/>
                        {/each}
                    </div>
                {/if}
            </div>

            <div class="column">
                <SectionLabel text="History"/>
                <div class="list">
                    {#each detail.revisions as revision (revision.id)}
                        <ListRow active={revision.latest} title={date(revision.createdAt)}
                                 badges={present(revision.latest && "Latest", revision.loaded && "Loaded")}
                                 subtitle={revision.changelog ?? (revision.first ? "First version" : "")}>
                            {#snippet meta()}{reports(revision.works, revision.fails)}{/snippet}
                        </ListRow>
                    {/each}
                </div>
            </div>
        </div>
    {/if}
</DetailPage>

<style lang="scss">
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
</style>
