<script lang="ts">
    import Dialog from "../../../common/Dialog.svelte";
    import PillButton from "../../../common/PillButton.svelte";
    import Chip from "../../../common/Chip.svelte";
    import SectionLabel from "../../../common/SectionLabel.svelte";
    import TextField from "../../../common/TextField.svelte";
    import SegmentedControl from "../../../common/SegmentedControl.svelte";
    import Address from "../../../common/Address.svelte";
    import {
        copyMarketplaceShareCode,
        deleteMarketplaceConfig,
        getCurrentServer,
        publishMarketplaceConfig,
        setMarketplaceConfigDetails,
        updateTrackedConfig
    } from "../../../../../integration/rest";
    import type {ConfigTracker, MarketplaceConfigDetails} from "../../../../../integration/types";
    import {attempt, dialog, type DialogRequest} from "../marketplace";

    type Kind = "New" | "Overlay" | "Fork";
    type Request = Exclude<DialogRequest, { kind: "load" }>;

    let {tracker, user, tags, onopen}: {
        tracker: ConfigTracker | null;
        user?: string | null;
        tags: string[];
        onopen: (id: number) => void;
    } = $props();

    // The last request stays while the dialog fades out
    let request = $state.raw<Request>({kind: "publish"});
    let seen: DialogRequest | null = null;
    let kind = $state<string>("New");
    let name = $state("");
    let description = $state("");
    let servers = $state("");
    let selectedTags = $state<string[]>([]);
    let visibility = $state("Public");
    let changelog = $state("");

    const open = $derived(!!$dialog && $dialog.kind !== "load");
    const editing = $derived(tracker?.state === "Editing");
    const kinds = $derived(editing ? (tracker?.own ? ["Overlay", "New"] : ["Overlay", "Fork", "New"]) : ["New"]);
    const form = $derived(request.kind === "publish" || request.kind === "edit");
    const shape = $derived.by(() => {
        switch (request.kind) {
            case "publish":
                return {title: "Publish config", width: 580, confirm: "Publish"};
            case "edit":
                return {title: `Edit ${request.detail.config.address}`, width: 580, confirm: "Save"};
            case "update":
                return {title: `Update ${tracker?.address ?? ""}`, width: 460, confirm: "Update"};
            case "delete":
                return {title: `Delete ${request.config.address}`, width: 440, confirm: "Delete"};
            case "published":
                return {title: "Published", width: 440, confirm: undefined};
        }
    });

    $effect.pre(() => {
        const next = $dialog;
        if (next === seen) {
            return;
        }
        seen = next;
        if (!next || next.kind === "load") {
            return;
        }

        request = next;
        if (next.kind === "publish") {
            kind = kinds[0];
            fill({name: "", description: "", servers: [], tags: [], visibility: "public"});
            insertServer();
        } else if (next.kind === "edit") {
            const {config} = next.detail;
            fill({...config, description: next.detail.description, visibility: config.visibility ?? "public"});
        }
    });

    function fill(details: MarketplaceConfigDetails) {
        name = details.name;
        description = details.description;
        servers = details.servers.join(", ");
        selectedTags = [...details.tags];
        visibility = details.visibility === "unlisted" ? "Unlisted" : "Public";
    }

    // The page stays loaded while the player joins and leaves servers, so it asks for the current one
    async function insertServer() {
        const server = (await attempt(getCurrentServer))?.rootDomain;
        if (server && servers === "") {
            servers = server;
        }
    }

    function details(): MarketplaceConfigDetails {
        return {
            name: name.trim(),
            description: description.trim(),
            tags: selectedTags,
            servers: servers.split(/[,\s]+/).filter(server => server !== ""),
            visibility: visibility === "Unlisted" ? "unlisted" : "public"
        };
    }

    function toggle(tag: string) {
        selectedTags = selectedTags.includes(tag) ? selectedTags.filter(t => t !== tag) : [...selectedTags, tag];
    }

    function close() {
        dialog.set(null);
    }

    async function confirm(): Promise<boolean> {
        switch (request.kind) {
            case "publish": {
                const published = await attempt(() => publishMarketplaceConfig(kind as Kind, details()));
                if (published) {
                    dialog.set({kind: "published", published});
                }
                return false;
            }
            case "edit": {
                const {config} = request.detail;
                const saved = await attempt(() => setMarketplaceConfigDetails(config.id, details()).then(() => true));
                if (saved) {
                    request.ondone();
                }
                return !!saved;
            }
            case "update": {
                const result = await attempt(() => updateTrackedConfig(changelog.trim()));
                if (result) {
                    changelog = "";
                }
                return !!result;
            }
            case "delete": {
                const {config} = request;
                const deleted = await attempt(() => deleteMarketplaceConfig(config.id).then(() => true));
                if (deleted) {
                    request.ondone();
                }
                return !!deleted;
            }
        }
        return false;
    }

    async function copy(id: number) {
        await attempt(() => copyMarketplaceShareCode(id));
    }
</script>

{#snippet published()}
    {#if request.kind === "published"}
        {@const result = request.published}
        <PillButton title="Open" onclick={() => { close(); onopen(result.id); }}/>
        {#if result.shareCode}
            <PillButton title="Copy code" primary onclick={() => copy(result.id)}/>
        {:else}
            <PillButton title="Done" primary onclick={close}/>
        {/if}
    {/if}
{/snippet}

<Dialog {open} onclose={close} onconfirm={confirm} title={shape.title} width={shape.width} confirm={shape.confirm}
        cancel={request.kind === "delete" ? "Keep it" : "Cancel"}
        danger={request.kind === "delete"}
        disabled={form && name.trim() === ""}
        footer={request.kind === "published" ? published : undefined}>
    {#if form}
        <div class="switches">
            {#if request.kind === "publish" && kinds.length > 1}
                <SegmentedControl options={kinds} value={kind} onchange={value => kind = value}/>
            {/if}
            <SegmentedControl options={["Public", "Unlisted"]} value={visibility} onchange={value => visibility = value}/>
        </div>
        {#if request.kind === "publish" && kind !== "New"}
            <div class="base">{kind === "Overlay" ? "On top of" : "Copy of"} {tracker?.address}</div>
        {/if}

        <div class="row">
            <div>
                <SectionLabel text="Name"/>
                <TextField bind:value={name} maxlength={64}
                           prefix={request.kind === "publish" && user ? `${user}/` : undefined}/>
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
                    <Chip text={tag} active={selectedTags.includes(tag)} onclick={() => toggle(tag)}/>
                {/each}
            </div>
        {/if}
    {:else if request.kind === "update"}
        <SectionLabel text="Changelog"/>
        <TextField bind:value={changelog} multiline/>
    {:else if request.kind === "delete"}
        <p>Removes it for everyone, with its history and reports.</p>
    {:else if request.kind === "published"}
        <div class="head">
            <span class="address"><Address address={request.published.address}/></span>
            {#if request.published.shareCode}
                <Chip text="Unlisted"/>
            {/if}
        </div>
        {#if request.published.shareCode}
            <div class="code">{request.published.shareCode}</div>
        {/if}
    {/if}
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

  p {
    line-height: 1.5;
    margin: 12px 0 0;
  }

  .head {
    display: flex;
    align-items: center;
    column-gap: 8px;
    padding-top: 12px;
  }

  .address {
    font-size: 14px;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .code {
    margin-top: 12px;
    font-family: monospace;
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 3px;
    text-align: center;
    color: var(--clickgui-text-color);
    background-color: var(--clickgui-input-background-color);
    border-bottom: solid 2px var(--clickgui-input-border-color);
    border-radius: 3px;
    padding: 12px;
  }
</style>
