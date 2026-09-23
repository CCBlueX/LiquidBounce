<script lang="ts">
    import Button from "./Button.svelte";
    import type {MarketplaceItem} from "../../../../integration/types";

    let {item, busy = false, oninstall, onupdate, onapply}: {
        item: MarketplaceItem;
        busy?: boolean;
        oninstall: () => void;
        onupdate: () => void;
        onapply: () => void;
    } = $props();
</script>

{#if item.installable}
    <Button title="Install" primary disabled={busy} onclick={oninstall}/>
{:else if item.update}
    <Button title="Update" primary disabled={busy} onclick={onupdate}/>
{:else if item.type === "Theme" && item.subscribed && !item.inUse}
    <Button title="Apply" primary disabled={busy} onclick={onapply}/>
{:else if !item.subscribed}
    <Button title="Install" primary disabled onclick={oninstall}/>
{/if}
