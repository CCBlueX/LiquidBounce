<script lang="ts">
    import {onMount} from "svelte";
    import DetailHead from "./DetailHead.svelte";
    import ItemAction from "./ItemAction.svelte";
    import Button from "./Button.svelte";
    import Tag from "./Tag.svelte";
    import Label from "./Label.svelte";
    import Message from "./Message.svelte";
    import Stat from "./Stat.svelte";
    import {
        applyMarketplaceTheme,
        getMarketplaceItemDetail,
        installMarketplaceItem,
        removeMarketplaceItem,
        updateMarketplaceItem
    } from "../../../../integration/rest";
    import type {MarketplaceItemDetail} from "../../../../integration/types";
    import {attempt, count, date, message, notifyInstalled, typeName, UNKNOWN_PACK, version} from "./marketplace";

    let {id, onback}: {
        id: number;
        onback: () => void;
    } = $props();

    let detail = $state<MarketplaceItemDetail | null>(null);
    let error = $state<string | null>(null);
    let busy = $state(false);

    const item = $derived(detail?.item);
    const newest = $derived(detail?.versions[0]?.revision);

    onMount(refresh);

    async function refresh() {
        error = null;
        try {
            detail = await getMarketplaceItemDetail(id);
        } catch (e) {
            error = message(e);
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

<div class="detail">
    {#if detail && item}
        <DetailHead image={item.image ?? UNKNOWN_PACK} {onback}>
            {#snippet title()}
                {item.name}{#if item.author}<span class="author">by {item.author}</span>{/if}
            {/snippet}

            {#snippet tags()}
                {#if item.subscribed}
                    <Tag text="Installed"/>
                {/if}
                {#if item.restartRequired}
                    <Tag text="Restart needed"/>
                {/if}
                {#if item.inUse}
                    <Tag text="In use"/>
                {/if}
                {#if item.featured}
                    <Tag text="Featured"/>
                {/if}
            {/snippet}

            {#snippet subtitle()}
                {item.subscribed && item.installed ? `${version(item.installed)} · ` : ""}{typeName(item.type)}
            {/snippet}

            {#snippet actions()}
                {#if item.subscribed}
                    <Button title="Remove" disabled={busy} onclick={() => run(() => removeMarketplaceItem(id))}/>
                {/if}
                <ItemAction {item} {busy}
                            oninstall={() => run(async () => notifyInstalled(await installMarketplaceItem(id)))}
                            onupdate={() => run(() => updateMarketplaceItem(id))}
                            onapply={() => run(() => applyMarketplaceTheme(id))}/>
            {/snippet}
        </DetailHead>

        <div class="body">
            <div class="stats">
                {#if item.subscribed && item.installed}
                    <Stat value={version(item.installed)} label="Installed"/>
                {/if}
                {#if newest}
                    <Stat value="{version(newest)}{newest.createdAt ? ` · ${date(newest.createdAt)}` : ''}"
                          label="Newest release"/>
                {/if}
                <Stat value={count(item.downloads)} label="Downloads"/>
                {#if item.rating !== undefined}
                    <Stat value={item.rating.toFixed(1)} label="{item.reviews} {item.reviews === 1 ? 'review' : 'reviews'}"/>
                {/if}
            </div>

            {#if detail.description || item.summary}
                <p class="description">{detail.description || item.summary}</p>
            {/if}

            <Label text="Versions"/>
            <div class="versions">
                {#each detail.versions as entry (entry.revision.id)}
                    <div class="version" class:installed={entry.installed} class:unfit={!entry.fits && !entry.installed}>
                        <span class="number">{version(entry.revision)}</span>
                        <span>{date(entry.revision.createdAt)}</span>
                        <span class="changelog">{entry.revision.changelog ?? ""}</span>
                        <span>{entry.revision.liquidbounce ?? ""}</span>
                        <span class="tag">
                            {#if entry.installed}
                                <Tag text="Installed"/>
                            {:else if !entry.fits}
                                Not for {detail.liquidbounce}
                            {/if}
                        </span>
                    </div>
                {/each}
            </div>
        </div>
    {:else if error}
        <DetailHead {onback}>
            {#snippet title()}Item{/snippet}
        </DetailHead>
        <Message title="Couldn't open this item">
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

  .author {
    margin-left: 6px;
    font-size: 13px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
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
