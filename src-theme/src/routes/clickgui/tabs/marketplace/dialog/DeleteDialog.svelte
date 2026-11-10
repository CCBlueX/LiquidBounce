<script lang="ts">
    import Button from "../../../common/Button.svelte";
    import Modal from "../../../common/Modal.svelte";
    import {deleteMarketplaceConfig} from "../../../../../integration/rest";
    import type {MarketplaceConfig} from "../../../../../integration/types";

    /**
     * [ondone] runs once the config is gone.
     */
    let {config, ondone, onclose}: {
        config: MarketplaceConfig;
        ondone: () => void;
        onclose: () => void;
    } = $props();

    let busy = $state(false);

    async function remove() {
        busy = true;
        // The client tells the player what went wrong
        const deleted = await deleteMarketplaceConfig(config.id).then(() => true, () => false);
        busy = false;
        if (deleted) {
            ondone();
            onclose();
        }
    }
</script>

<Modal title="Delete {config.address}" width={420} {onclose}>
    <p class="hint">Removes it for everyone, with its history and reports.</p>

    {#snippet footer()}
        <Button variant="secondary" autofocus onclick={onclose}>Keep it</Button>
        <Button variant="danger" disabled={busy} onclick={remove}>Delete</Button>
    {/snippet}
</Modal>

<style lang="scss">
  .hint {
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
