<script lang="ts">
    import Switch from "../../setting/common/Switch.svelte";
    import Dialog from "./Dialog.svelte";
    import Button from "./Button.svelte";
    import Chip from "./Chip.svelte";
    import Tag from "./Tag.svelte";
    import Label from "./Label.svelte";
    import Row from "./Row.svelte";
    import Input from "./Input.svelte";
    import {getMarketplaceLoadPlan, loadMarketplaceConfig} from "../../../../integration/rest";
    import type {MarketplaceLinkedConfig, MarketplaceLoadPlan} from "../../../../integration/types";
    import {attempt, notify, typeName, UNKNOWN_PACK, version} from "./marketplace";

    let {open = $bindable(), config}: {
        open: boolean;
        config: MarketplaceLinkedConfig | null;
    } = $props();

    let plan = $state<MarketplaceLoadPlan | null>(null);
    let pick = $state(false);
    let modules = $state<string[]>([]);
    let filter = $state("");
    let loading = $state(false);

    const shown = $derived(plan?.modules.filter(module => module.toLowerCase().includes(filter.trim().toLowerCase())) ?? []);

    $effect(() => {
        if (open && config) {
            fetchPlan(config.id);
        }
    });

    async function fetchPlan(id: number) {
        plan = null;
        pick = false;
        filter = "";
        plan = await attempt(() => getMarketplaceLoadPlan(id)) ?? null;
        if (plan) {
            modules = [...plan.modules];
        } else {
            open = false;
        }
    }

    function toggle(module: string) {
        modules = modules.includes(module) ? modules.filter(m => m !== module) : [...modules, module];
    }

    async function load() {
        if (!config || !plan || loading) {
            return;
        }

        loading = true;
        const result = await attempt(() => loadMarketplaceConfig(config.id, pick ? modules : null));
        loading = false;
        if (!result) {
            return;
        }

        open = false;
        notify([
            pick ? `Loaded ${modules.length} modules from ${config.address}.` : `Loaded ${config.address}.`,
            result.installed.length > 0 ? `Installed ${result.installed.join(", ")}.` : ""
        ].filter(Boolean).join(" "));
    }
</script>

<Dialog bind:open title="Load {config?.address ?? ''}" width={560}>
    {#if plan}
        {#if plan.installs.length > 0}
            <Label text="Installs"/>
            <div class="list">
                {#each plan.installs as install (install.id)}
                    <Row image={install.image ?? UNKNOWN_PACK}>
                        {#snippet title()}{install.name}{/snippet}
                        {#snippet tags()}
                            {#if install.restart}
                                <Tag text="Restart needed"/>
                            {/if}
                        {/snippet}
                        {#snippet subtitle()}{typeName(install.type)} · {version(install.revision)}{/snippet}
                    </Row>
                {/each}
            </div>
        {/if}

        {#if plan.leftOut.length > 0}
            <Label text="Left out"/>
            <div class="list">
                {#each plan.leftOut as leftOut (leftOut.id)}
                    <Row image={leftOut.image ?? UNKNOWN_PACK}>
                        {#snippet title()}{leftOut.name}{/snippet}
                        {#snippet subtitle()}{typeName(leftOut.type)}{/snippet}
                    </Row>
                {/each}
            </div>
        {/if}

        {#if plan.modules.length > 0}
            <div class="pick">
                <Switch name="Only some modules" bind:value={pick}/>
                {#if pick}
                    <span class="count">{modules.length}/{plan.modules.length}</span>
                {/if}
            </div>
            {#if pick}
                <Input bind:value={filter} placeholder="Filter"/>
                <div class="chips">
                    {#each shown as module (module)}
                        <Chip text={module} active={modules.includes(module)} onclick={() => toggle(module)}/>
                    {/each}
                </div>
            {/if}
        {/if}
    {/if}

    {#snippet footer()}
        <Button title="Cancel" onclick={() => open = false}/>
        <Button title="Load" primary disabled={!plan || loading || (pick && modules.length === 0)} onclick={load}/>
    {/snippet}
</Dialog>

<style lang="scss">
  .list {
    border-radius: 5px;
    overflow: hidden;
    background-color: var(--clickgui-module-settings-background-color);
  }

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
