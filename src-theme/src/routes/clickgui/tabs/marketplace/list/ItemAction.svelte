<script lang="ts">
    import PillButton from "../ui/PillButton.svelte";
    import type {MarketplaceItem} from "../../../../../integration/types";

    let {item, busy = false, oninstall, onupdate, onapply}: {
        item: MarketplaceItem;
        busy?: boolean;
        oninstall: () => void;
        onupdate: () => void;
        onapply: () => void;
    } = $props();
</script>

{#if item.installable}
    <PillButton title="Install" primary disabled={busy} onclick={oninstall}/>
{:else if item.update}
    <PillButton title="Update" primary disabled={busy} onclick={onupdate}/>
{:else if item.type === "Theme" && item.subscribed && !item.inUse}
    <PillButton title="Apply" primary disabled={busy} onclick={onapply}/>
{:else if !item.subscribed}
    <PillButton title="Install" primary disabled onclick={oninstall}/>
{/if}
