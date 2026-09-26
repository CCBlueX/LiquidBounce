<script lang="ts">
    import Dialog from "./Dialog.svelte";
    import Button from "./Button.svelte";
    import {deleteMarketplaceConfig} from "../../../../integration/rest";
    import type {MarketplaceConfig} from "../../../../integration/types";
    import {attempt, notify} from "./marketplace";

    let {open = $bindable(), config, ondeleted}: {
        open: boolean;
        config: MarketplaceConfig;
        ondeleted: () => void;
    } = $props();

    let loading = $state(false);

    async function remove() {
        if (loading) {
            return;
        }

        loading = true;
        const deleted = await attempt(async () => {
            await deleteMarketplaceConfig(config.id);
            return true;
        });
        loading = false;

        if (deleted) {
            open = false;
            notify(`Deleted ${config.address}`);
            ondeleted();
        }
    }
</script>

<Dialog bind:open title="Delete {config.address}" width={440}>
    <p>Removes it for everyone, with its history and reports.</p>

    {#snippet footer()}
        <Button title="Keep it" onclick={() => open = false}/>
        <Button title="Delete" danger disabled={loading} onclick={remove}/>
    {/snippet}
</Dialog>

<style lang="scss">
  p {
    line-height: 1.5;
    margin: 12px 0 0;
  }
</style>
