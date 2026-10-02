<script lang="ts">
    import Switch from "../../../setting/common/Switch.svelte";
    import Dialog from "../../../common/Dialog.svelte";
    import Chip from "../../../common/Chip.svelte";
    import TextField from "../../../common/TextField.svelte";
    import {getMarketplaceConfigModules, loadMarketplaceConfig} from "../../../../../integration/rest";
    import {dialog, type DialogRequest} from "../marketplace";

    // The last config stays while the dialog fades out
    let config = $state.raw<{ id: number; address: string } | null>(null);
    let seen: DialogRequest | null = null;
    let available = $state<string[]>([]);
    let pick = $state(false);
    let modules = $state<string[]>([]);
    let filter = $state("");

    const open = $derived($dialog?.kind === "load");
    const shown = $derived(available.filter(module => module.toLowerCase().includes(filter.trim().toLowerCase())));

    $effect.pre(() => {
        const next = $dialog;
        if (next !== seen) {
            seen = next;
            if (next?.kind === "load") {
                config = next.config;
                fetchModules(next.config.id);
            }
        }
    });

    async function fetchModules(id: number) {
        available = [];
        pick = false;
        filter = "";
        // Without them, the whole config still loads
        available = await getMarketplaceConfigModules(id).catch(() => []);
        modules = [...available];
    }

    function toggle(module: string) {
        modules = modules.includes(module) ? modules.filter(m => m !== module) : [...modules, module];
    }

    // The client tells the player what the load did
    async function load(): Promise<boolean> {
        return await loadMarketplaceConfig(config!!.id, pick ? modules : null).then(() => true, () => false);
    }
</script>

<Dialog {open} onclose={() => dialog.set(null)} title="Load {config?.address ?? ''}" width={560}
        confirm="Load" disabled={pick && modules.length === 0} onconfirm={load}>
    {#if available.length > 0}
        <div class="pick">
            <Switch name="Only some modules" bind:value={pick}/>
            {#if pick}
                <span class="count">{modules.length}/{available.length}</span>
            {/if}
        </div>
        {#if pick}
            <TextField bind:value={filter} placeholder="Filter"/>
            <div class="chips">
                {#each shown as module (module)}
                    <Chip text={module} active={modules.includes(module)} onclick={() => toggle(module)}/>
                {/each}
            </div>
        {/if}
    {/if}
</Dialog>

<style lang="scss">
  .pick {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 0 8px;
  }

  .count {
    font-family: monospace;
    font-size: 12px;
    color: var(--clickgui-text-dimmed-color);
  }

  .chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 8px;
    max-height: 120px;
    overflow: auto;
  }
</style>
