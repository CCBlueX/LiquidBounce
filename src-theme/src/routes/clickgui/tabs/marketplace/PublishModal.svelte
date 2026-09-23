<script lang="ts">
    import Dialog from "./Dialog.svelte";
    import Button from "./Button.svelte";
    import Chip from "./Chip.svelte";
    import Label from "./Label.svelte";
    import Input from "./Input.svelte";
    import Segmented from "./Segmented.svelte";
    import {publishMarketplaceConfig} from "../../../../integration/rest";
    import type {ConfigTracker, MarketplaceContext, MarketplacePublished} from "../../../../integration/types";
    import {attempt} from "./marketplace";

    type Kind = "New" | "Overlay" | "Fork";

    let {open = $bindable(), tracker, context, tags, onpublished}: {
        open: boolean;
        tracker: ConfigTracker | null;
        context: MarketplaceContext | null;
        tags: string[];
        onpublished: (published: MarketplacePublished) => void;
    } = $props();

    let kind = $state<string>("New");
    let name = $state("");
    let description = $state("");
    let servers = $state("");
    let selectedTags = $state<string[]>([]);
    let visibility = $state("Public");
    let loading = $state(false);

    const editing = $derived(tracker?.state === "Editing");
    const kinds = $derived(editing ? (tracker?.own ? ["Overlay", "New"] : ["Overlay", "Fork", "New"]) : ["New"]);

    let wasOpen = false;
    $effect(() => {
        if (open && !wasOpen) {
            kind = kinds[0];
            name = "";
            description = "";
            servers = context?.server ?? "";
            selectedTags = [];
            visibility = "Public";
        }
        wasOpen = open;
    });

    function toggle(tag: string) {
        selectedTags = selectedTags.includes(tag) ? selectedTags.filter(t => t !== tag) : [...selectedTags, tag];
    }

    async function publish() {
        if (loading) {
            return;
        }

        loading = true;
        const published = await attempt(() => publishMarketplaceConfig(kind as Kind, {
            name: name.trim(),
            description: description.trim(),
            tags: selectedTags,
            servers: servers.split(/[,\s]+/).filter(server => server !== ""),
            visibility: visibility === "Unlisted" ? "unlisted" : "public"
        }));
        loading = false;

        if (published) {
            open = false;
            onpublished(published);
        }
    }
</script>

<Dialog bind:open title="Publish config" width={580}>
    <div class="switches">
        {#if kinds.length > 1}
            <Segmented options={kinds} value={kind} onchange={value => kind = value}/>
        {/if}
        <Segmented options={["Public", "Unlisted"]} value={visibility} onchange={value => visibility = value}/>
    </div>
    {#if kind !== "New"}
        <div class="base">{kind === "Overlay" ? "On top of" : "Copy of"} {tracker?.address}</div>
    {/if}

    <div class="row">
        <div>
            <Label text="Name"/>
            <Input bind:value={name} prefix={context?.user ? `${context.user}/` : undefined} maxlength={64}/>
        </div>
        <div>
            <Label text="Servers"/>
            <Input bind:value={servers}/>
        </div>
    </div>

    <Label text="Description"/>
    <Input bind:value={description} multiline/>

    {#if tags.length > 0}
        <Label text="Tags"/>
        <div class="chips">
            {#each tags as tag (tag)}
                <Chip text={tag} active={selectedTags.includes(tag)} onclick={() => toggle(tag)}/>
            {/each}
        </div>
    {/if}

    {#snippet footer()}
        <Button title="Cancel" onclick={() => open = false}/>
        <Button title="Publish" primary disabled={loading || name.trim() === ""} onclick={publish}/>
    {/snippet}
</Dialog>

<style lang="scss">
  .switches {
    display: flex;
    justify-content: space-between;
    padding-top: 12px;
  }

  .base {
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    padding: 8px 0 0 4px;
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
