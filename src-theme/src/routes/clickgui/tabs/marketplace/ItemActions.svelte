<script lang="ts">
    import Button from "../../common/Button.svelte";
    import {
        applyMarketplaceTheme,
        installMarketplaceItem,
        removeMarketplaceItem,
        updateMarketplaceItem
    } from "../../../../integration/rest";
    import type {MarketplaceItem} from "../../../../integration/types";

    /**
     * What an add-on, script or theme can do next. [removable] adds removing an installed one. [onchange] runs
     * after an action, whether it went through or not.
     */
    let {item, removable = false, onchange}: {
        item: MarketplaceItem;
        removable?: boolean;
        onchange: () => void;
    } = $props();

    let busy = $state(false);

    async function run(action: (id: number) => Promise<unknown>) {
        busy = true;
        // The client tells the player what went wrong
        await action(item.id).catch(() => null);
        busy = false;
        onchange();
    }
</script>

{#if removable && item.subscribed}
    <Button variant="secondary" disabled={busy} onclick={() => run(removeMarketplaceItem)}>Remove</Button>
{/if}
{#if item.update}
    <Button disabled={busy} onclick={() => run(updateMarketplaceItem)}>Update</Button>
{:else if item.type === "Theme" && item.subscribed && !item.inUse}
    <Button disabled={busy} onclick={() => run(applyMarketplaceTheme)}>Apply</Button>
{:else if !item.subscribed}
    <Button disabled={busy || !item.installable} onclick={() => run(installMarketplaceItem)}>Install</Button>
{/if}
