<script lang="ts">
    import {onMount} from "svelte";
    import DetailPage from "./DetailPage.svelte";
    import ItemAction from "../ItemAction.svelte";
    import PillButton from "../ui/PillButton.svelte";
    import Badge from "../ui/Badge.svelte";
    import SectionLabel from "../ui/SectionLabel.svelte";
    import {
        applyMarketplaceTheme,
        getMarketplaceItemDetail,
        installMarketplaceItem,
        removeMarketplaceItem,
        updateMarketplaceItem
    } from "../../../../../integration/rest";
    import type {MarketplaceItemDetail} from "../../../../../integration/types";
    import {
        attempt,
        itemBadges,
        notifyInstalled,
        reviews,
        typeName,
        UNKNOWN_PACK,
        version
    } from "../marketplace";
    import {compactNumber, date, errorMessage} from "../../../../../util/utils";

    let {id, onback}: {
        id: number;
        onback: () => void;
    } = $props();

    let detail = $state<MarketplaceItemDetail | null>(null);
    let error = $state<string | null>(null);
    let busy = $state(false);

    const item = $derived(detail?.item);
    const newest = $derived(detail?.versions[0]?.revision);
    const installed = $derived(item?.subscribed ? item.installed : undefined);
    const stats = $derived(item ? [
        {label: "Installed", value: installed && version(installed)},
        {label: "Newest release", value: newest && `${version(newest)}${newest.createdAt ? ` · ${date(newest.createdAt)}` : ""}`},
        {label: "Downloads", value: compactNumber(item.downloads)},
        {label: reviews(item.reviews), value: item.rating?.toFixed(1)}
    ] : []);

    onMount(refresh);

    async function refresh() {
        error = null;
        try {
            detail = await getMarketplaceItemDetail(id);
        } catch (e) {
            error = errorMessage(e);
        }
    }

    async function run(action: () => Promise<unknown>) {
        if (busy) {
            return;
        }

        busy = true;
        await attempt(action);
        await refresh();
        busy = false;
    }
</script>

<DetailPage noun="item" loaded={!!item} {error} onretry={refresh} {onback} {stats}
            image={item?.image ?? UNKNOWN_PACK} title={item?.name} by={item?.author}
            badges={item ? itemBadges(item, item.subscribed && "Installed") : []}
            subtitle="{installed ? `${version(installed)} · ` : ''}{item ? typeName(item.type) : ''}"
            description={detail?.description || item?.summary}>
    {#snippet actions()}
        {#if item?.subscribed}
            <PillButton title="Remove" disabled={busy} onclick={() => run(() => removeMarketplaceItem(id))}/>
        {/if}
        {#if item}
            <ItemAction {item} {busy}
                        oninstall={() => run(async () => notifyInstalled(await installMarketplaceItem(id)))}
                        onupdate={() => run(() => updateMarketplaceItem(id))}
                        onapply={() => run(() => applyMarketplaceTheme(id))}/>
        {/if}
    {/snippet}

    {#if detail}
        <SectionLabel text="Versions"/>
        <div class="versions">
            {#each detail.versions as entry (entry.revision.id)}
                <div class="version" class:installed={entry.installed} class:unfit={!entry.fits && !entry.installed}>
                    <span class="number">{version(entry.revision)}</span>
                    <span>{date(entry.revision.createdAt)}</span>
                    <span class="changelog">{entry.revision.changelog ?? ""}</span>
                    <span>{entry.revision.liquidbounce ?? ""}</span>
                    <span class="tag">
                        {#if entry.installed}
                            <Badge text="Installed"/>
                        {:else if !entry.fits}
                            Not for {detail.liquidbounce}
                        {/if}
                    </span>
                </div>
            {/each}
        </div>
    {/if}
</DetailPage>

<style lang="scss">
  .versions {
    display: flex;
    flex-direction: column;
    border-radius: 5px;
    overflow: hidden;
    background-color: var(--clickgui-module-settings-background-color);
  }

  .version {
    display: grid;
    grid-template-columns: 80px 100px 1fr max-content 150px;
    align-items: center;
    column-gap: 12px;
    padding: 9px 14px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    border-left: solid 3px transparent;
    border-bottom: solid 1px var(--clickgui-global-settings-divider-color);

    &:last-child {
      border-bottom: none;
    }

    &.installed {
      background-color: var(--clickgui-tab-active-background-color);
      border-left-color: var(--clickgui-module-settings-border-color);
    }

    &.unfit {
      opacity: 0.5;
    }
  }

  .number {
    font-family: monospace;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .changelog {
    color: var(--clickgui-text-color);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tag {
    display: flex;
    justify-content: flex-end;
  }
</style>
