<script lang="ts">
    import Row from "./Row.svelte";
    import Tag from "./Tag.svelte";
    import Button from "./Button.svelte";
    import ItemAction from "./ItemAction.svelte";
    import type {MarketplaceItem} from "../../../../integration/types";
    import {count, UNKNOWN_PACK, version} from "./marketplace";

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
    <Button title="Remove" disabled={busy} onclick={onremove}/>
{/snippet}

<Row image={item.image ?? UNKNOWN_PACK} onclick={onopen} hover={item.subscribed ? remove : undefined} dim={!!item.notFor}>
    {#snippet title()}
        {item.name}{#if item.author}<span class="author">by {item.author}</span>{/if}
    {/snippet}

    {#snippet tags()}
        {#if item.subscribed && item.installed}
            <Tag text={version(item.installed)}/>
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
</Row>

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
