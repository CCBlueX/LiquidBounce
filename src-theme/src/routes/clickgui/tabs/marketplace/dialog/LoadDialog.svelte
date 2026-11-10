<script lang="ts">
    import {onMount} from "svelte";
    import Button from "../../../common/Button.svelte";
    import Chip from "../../../common/Chip.svelte";
    import Modal from "../../../common/Modal.svelte";
    import TextInput from "../../../common/TextInput.svelte";
    import Switch from "../../../setting/common/Switch.svelte";
    import {getMarketplaceConfigModules, loadMarketplaceConfig} from "../../../../../integration/rest";
    import type {MarketplaceConfig} from "../../../../../integration/types";

    let {config, onclose}: {
        config: Pick<MarketplaceConfig, "id" | "address">;
        onclose: () => void;
    } = $props();

    let modules = $state<string[]>([]);
    let partial = $state(false);
    let picked = $state<string[]>([]);
    let filter = $state("");
    let busy = $state(false);

    const shown = $derived(modules.filter(module => module.toLowerCase().includes(filter.trim().toLowerCase())));

    onMount(async () => {
        // Without the list, the whole config still loads
        modules = await getMarketplaceConfigModules(config.id).catch(() => []);
    });

    function toggle(module: string) {
        picked = picked.includes(module) ? picked.filter(other => other !== module) : [...picked, module];
    }

    async function load() {
        busy = true;
        // The client tells the player how the load went
        const loaded = await loadMarketplaceConfig(config.id, partial ? picked : null).then(() => true, () => false);
        busy = false;
        if (loaded) {
            onclose();
        }
    }
</script>

<Modal title="Load {config.address}" width={540} {onclose}>
    <p class="hint">
        {#if partial}
            Only the picked modules change, and the config is not tracked.
        {:else}
            Loads the whole config and tracks it, so you can revert your edits or go back to your settings.
        {/if}
    </p>

    {#if modules.length > 0}
        <div class="partial">
            <Switch name="Only some modules" bind:value={partial}/>
            {#if partial}
                <span class="count">{picked.length}/{modules.length}</span>
                <Button variant="secondary" onclick={() => picked = [...modules]}>All</Button>
                <Button variant="secondary" onclick={() => picked = []}>None</Button>
            {/if}
        </div>
        {#if partial}
            <TextInput bind:value={filter} placeholder="Filter modules"/>
            <div class="modules">
                {#each shown as module (module)}
                    <Chip active={picked.includes(module)} onclick={() => toggle(module)}>{module}</Chip>
                {/each}
            </div>
        {/if}
    {/if}

    {#snippet footer()}
        <Button variant="secondary" onclick={onclose}>Cancel</Button>
        <Button autofocus disabled={busy || (partial && picked.length === 0)} onclick={load}>Load</Button>
    {/snippet}
</Modal>

<style lang="scss">
  .hint {
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }

  .partial {
    display: flex;
    align-items: center;
    column-gap: 6px;
    min-height: 26px;
  }

  .count {
    flex: 1;
    margin-left: 4px;
    font-family: monospace;
    font-weight: 500;
    letter-spacing: 1px;
  }

  .modules {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    max-height: 220px;
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 4px;
    }
  }
</style>
