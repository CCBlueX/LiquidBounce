<script lang="ts">
    import {onMount, untrack} from "svelte";
    import Button from "../../../common/Button.svelte";
    import Chip from "../../../common/Chip.svelte";
    import Modal from "../../../common/Modal.svelte";
    import TextInput from "../../../common/TextInput.svelte";
    import {editMarketplaceConfig, publishMarketplaceConfig} from "../../../../../integration/rest";
    import type {
        MarketplaceConfigDetail,
        MarketplaceConfigListing,
        MarketplacePublishKind,
        MarketplaceVisibility
    } from "../../../../../integration/types";
    import {currentServer, dialog, refreshCurrentServer, tracker} from "../marketplace";

    const NAME_LENGTH = 64;
    const SERVER_SEPARATOR = /[,\s]+/;

    /**
     * Publishes the settings as a config, or with [editing] changes the listing of that config. [ondone] runs
     * once an edit went through.
     */
    let {editing, tags, ondone, onclose}: {
        editing?: MarketplaceConfigDetail;
        tags: string[];
        ondone?: () => void;
        onclose: () => void;
    } = $props();

    // Edits to a tracked config can go on top of it or become a copy, and a copy of an own config is an update
    const kinds: MarketplacePublishKind[] = $tracker?.state !== "Editing"
        ? ["New"]
        : $tracker.own ? ["Overlay", "New"] : ["Overlay", "Fork", "New"];

    // The form starts from the listing and is the player's from then on
    const initial = untrack(() => editing);

    let kind = $state(kinds[0]);
    let name = $state(initial?.config.name ?? "");
    let description = $state(initial?.description ?? "");
    let servers = $state(initial?.config.servers.join(", ") ?? "");
    let pickedTags = $state(initial?.config.tags ?? []);
    let visibility = $state<MarketplaceVisibility>(initial?.config.visibility ?? "public");
    let busy = $state(false);

    onMount(async () => {
        if (editing) {
            return;
        }

        // The page stays loaded while the player joins and leaves servers
        await refreshCurrentServer();
        if ($currentServer && servers === "") {
            servers = $currentServer;
        }
    });

    function toggleTag(tag: string) {
        pickedTags = pickedTags.includes(tag) ? pickedTags.filter(other => other !== tag) : [...pickedTags, tag];
    }

    async function submit() {
        const listing: MarketplaceConfigListing = {
            name: name.trim(),
            description: description.trim(),
            tags: pickedTags,
            servers: servers.split(SERVER_SEPARATOR).filter(server => server !== ""),
            visibility
        };

        busy = true;
        // The client tells the player what went wrong
        if (editing) {
            const saved = await editMarketplaceConfig(editing.config.id, listing).then(() => true, () => false);
            if (saved) {
                ondone?.();
                onclose();
            }
        } else {
            const published = await publishMarketplaceConfig(kind, listing).catch(() => null);
            if (published) {
                $dialog = {kind: "published", config: published};
            }
        }
        busy = false;
    }
</script>

<Modal title={editing ? `Edit ${editing.config.address}` : "Publish config"} width={560} {onclose}>
    {#if !editing && kinds.length > 1}
        <div class="choice">
            <span class="label">Publish as</span>
            <div class="chips">
                {#each kinds as option (option)}
                    <Chip active={kind === option} onclick={() => kind = option}>{option}</Chip>
                {/each}
            </div>
            <span class="hint">
                {#if kind === "Overlay"}
                    Only what you changed. It loads on top of {$tracker?.address} and follows its updates.
                {:else if kind === "Fork"}
                    A copy of {$tracker?.address} with your changes.
                {:else}
                    All of your settings as a config of its own.
                {/if}
            </span>
        </div>
    {/if}

    <div class="row">
        <TextInput bind:value={name} label="Name" maxlength={NAME_LENGTH} autofocus/>
        <TextInput bind:value={servers} label="Servers" placeholder="Any server"/>
    </div>

    <TextInput bind:value={description} label="Description" multiline/>

    {#if tags.length > 0}
        <div class="choice">
            <span class="label">Tags</span>
            <div class="chips">
                {#each tags as tag (tag)}
                    <Chip active={pickedTags.includes(tag)} onclick={() => toggleTag(tag)}>{tag}</Chip>
                {/each}
            </div>
        </div>
    {/if}

    <div class="choice">
        <span class="label">Visibility</span>
        <div class="chips">
            <Chip active={visibility === "public"} onclick={() => visibility = "public"}>Public</Chip>
            <Chip active={visibility === "unlisted"} onclick={() => visibility = "unlisted"}>Unlisted</Chip>
        </div>
        <span class="hint">
            {visibility === "public" ? "Everyone finds it in the list." : "Only who has its share code finds it."}
        </span>
    </div>

    {#snippet footer()}
        <Button variant="secondary" onclick={onclose}>Cancel</Button>
        <Button disabled={busy || name.trim() === ""} onclick={submit}>{editing ? "Save" : "Publish"}</Button>
    {/snippet}
</Modal>

<style lang="scss">
  .row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    column-gap: 12px;
  }

  .choice {
    display: flex;
    flex-direction: column;
    row-gap: 6px;
  }

  .label {
    font-weight: 500;
    color: var(--clickgui-text-color);
  }

  .chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .hint {
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
