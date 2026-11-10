<script lang="ts">
    import {onMount} from "svelte";
    import Button from "../../../common/Button.svelte";
    import Chip from "../../../common/Chip.svelte";
    import Modal from "../../../common/Modal.svelte";
    import TextInput from "../../../common/TextInput.svelte";
    import {getMarketplaceConfigModules, loadMarketplaceConfig} from "../../../../../integration/rest";
    import type {MarketplaceConfig} from "../../../../../integration/types";

    const MAX_MATCHES = 6;

    let {config, onclose}: {
        config: Pick<MarketplaceConfig, "id" | "address">;
        onclose: () => void;
    } = $props();

    let modules = $state<string[]>([]);
    let picked = $state<string[]>([]);
    let search = $state("");
    let busy = $state(false);

    const matches = $derived.by(() => {
        const term = search.trim().toLowerCase();
        if (!term) {
            return [];
        }

        return modules
            .filter(module => !picked.includes(module) && module.toLowerCase().includes(term))
            .sort((a, b) => Number(b.toLowerCase().startsWith(term)) - Number(a.toLowerCase().startsWith(term)))
            .slice(0, MAX_MATCHES);
    });

    onMount(async () => {
        // Without the list, the whole config still loads
        modules = await getMarketplaceConfigModules(config.id).catch(() => []);
    });

    function pick(module: string) {
        picked = [...picked, module];
        search = "";
    }

    function handleSubmit() {
        if (matches.length > 0) {
            pick(matches[0]);
        }
    }

    async function load() {
        busy = true;
        // The client tells the player how the load went
        const loaded = await loadMarketplaceConfig(config.id, picked.length > 0 ? picked : null)
            .then(() => true, () => false);
        busy = false;
        if (loaded) {
            onclose();
        }
    }
</script>

<Modal title="Load {config.address}" width={460} {onclose}>
    <p class="hint">
        {#if picked.length > 0}
            Only these modules change, and the config is not tracked.
        {:else}
            Loads the whole config and tracks it, so you can revert your edits or go back to your settings.
        {/if}
    </p>

    {#if modules.length > 0}
        <TextInput bind:value={search} placeholder="Or search a module to load only some" onsubmit={handleSubmit}/>
        {#if matches.length > 0 || picked.length > 0}
            <div class="modules">
                {#each picked as module (module)}
                    <Chip active onclick={() => picked = picked.filter(other => other !== module)}>{module}</Chip>
                {/each}
                {#each matches as module (module)}
                    <Chip onclick={() => pick(module)}>{module}</Chip>
                {/each}
            </div>
        {/if}
    {/if}

    {#snippet footer()}
        <Button variant="secondary" onclick={onclose}>Cancel</Button>
        <Button autofocus disabled={busy} onclick={load}>Load</Button>
    {/snippet}
</Modal>

<style lang="scss">
  .hint {
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }

  .modules {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
</style>
