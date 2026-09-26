<script lang="ts">
    import Dialog from "../ui/Dialog.svelte";
    import PillButton from "../ui/PillButton.svelte";
    import ToggleChip from "../ui/ToggleChip.svelte";
    import SectionLabel from "../ui/SectionLabel.svelte";
    import TextField from "../ui/TextField.svelte";
    import SegmentedControl from "../ui/SegmentedControl.svelte";
    import {setMarketplaceConfigDetails} from "../../../../../integration/rest";
    import type {MarketplaceConfigDetail} from "../../../../../integration/types";
    import {attempt, notify} from "../marketplace";

    let {open = $bindable(), detail, tags, onsaved}: {
        open: boolean;
        detail: MarketplaceConfigDetail;
        tags: string[];
        onsaved: () => void;
    } = $props();

    let name = $state("");
    let description = $state("");
    let servers = $state("");
    let selectedTags = $state<string[]>([]);
    let visibility = $state("Public");
    let loading = $state(false);

    let wasOpen = false;
    $effect(() => {
        if (open && !wasOpen) {
            name = detail.config.name;
            description = detail.description;
            servers = detail.config.servers.join(", ");
            selectedTags = [...detail.config.tags];
            visibility = detail.config.visibility === "unlisted" ? "Unlisted" : "Public";
        }
        wasOpen = open;
    });

    function toggle(tag: string) {
        selectedTags = selectedTags.includes(tag) ? selectedTags.filter(t => t !== tag) : [...selectedTags, tag];
    }

    async function save() {
        if (loading) {
            return;
        }

        loading = true;
        const saved = await attempt(async () => {
            await setMarketplaceConfigDetails(detail.config.id, {
                name: name.trim(),
                description,
                tags: selectedTags,
                servers: servers.split(/[,\s]+/).filter(server => server !== ""),
                visibility: visibility === "Unlisted" ? "unlisted" : "public"
            });
            return true;
        });
        loading = false;

        if (saved) {
            open = false;
            notify(`Saved ${name.trim()}`);
            onsaved();
        }
    }
</script>

<Dialog bind:open title="Edit {detail.config.address}" width={580}>
    <div class="switches">
        <SegmentedControl options={["Public", "Unlisted"]} value={visibility} onchange={value => visibility = value}/>
    </div>

    <div class="row">
        <div>
            <SectionLabel text="Name"/>
            <TextField bind:value={name} maxlength={64}/>
        </div>
        <div>
            <SectionLabel text="Servers"/>
            <TextField bind:value={servers}/>
        </div>
    </div>

    <SectionLabel text="Description"/>
    <TextField bind:value={description} multiline/>

    {#if tags.length > 0}
        <SectionLabel text="Tags"/>
        <div class="chips">
            {#each tags as tag (tag)}
                <ToggleChip text={tag} active={selectedTags.includes(tag)} onclick={() => toggle(tag)}/>
            {/each}
        </div>
    {/if}

    {#snippet footer()}
        <PillButton title="Cancel" onclick={() => open = false}/>
        <PillButton title="Save" primary disabled={loading || name.trim() === ""} onclick={save}/>
    {/snippet}
</Dialog>

<style lang="scss">
  .switches {
    display: flex;
    padding-top: 12px;
  }

  .row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    column-gap: 14px;
  }

  .chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
</style>
