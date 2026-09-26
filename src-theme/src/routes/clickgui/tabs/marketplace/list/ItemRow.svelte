<script lang="ts">
    import ListRow from "../ui/ListRow.svelte";
    import Badge from "../ui/Badge.svelte";
    import PillButton from "../ui/PillButton.svelte";
    import ItemAction from "./ItemAction.svelte";
    import type {MarketplaceItem} from "../../../../../integration/types";
    import {count, UNKNOWN_PACK, version} from "../marketplace";

    let {item, busy = false, oninstall, onupdate, onremove, onapply, onopen}: {
        item: MarketplaceItem;
        busy?: boolean;
        oninstall: () => void;
        onupdate: () => void;
        onremove: () => void;
        onapply: () => void;
        onopen?: () => void;
    } = $props();
</script>

{#snippet remove()}
    <PillButton title="Remove" disabled={busy} onclick={onremove}/>
{/snippet}

<ListRow image={item.image ?? UNKNOWN_PACK} onclick={onopen} hover={item.subscribed ? remove : undefined} dim={!!item.notFor}>
    {#snippet title()}
        {item.name}{#if item.author}<span class="author">by {item.author}</span>{/if}
    {/snippet}

    {#snippet tags()}
        {#if item.subscribed && item.installed}
            <Badge text={version(item.installed)}/>
        {/if}
        {#if item.restartRequired}
            <Badge text="Restart needed"/>
        {/if}
        {#if item.inUse}
            <Badge text="In use"/>
        {/if}
        {#if item.featured}
            <Badge text="Featured"/>
        {/if}
    {/snippet}

    {#snippet subtitle()}{item.summary}{/snippet}

    {#snippet meta()}
        {#if item.notFor}
            <span>Not for {item.notFor}</span>
        {/if}
        {#if item.rating !== undefined}
            <span>{item.rating.toFixed(1)} from {item.reviews} {item.reviews === 1 ? "review" : "reviews"}</span>
        {/if}
        <span class="downloads">{count(item.downloads)} downloads</span>
    {/snippet}

    {#snippet primary()}
        <ItemAction {item} {busy} {oninstall} {onupdate} {onapply}/>
    {/snippet}
</ListRow>

<style lang="scss">
  .author {
    margin-left: 5px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
  }

  .downloads {
    min-width: 90px;
    text-align: right;
  }
</style>
