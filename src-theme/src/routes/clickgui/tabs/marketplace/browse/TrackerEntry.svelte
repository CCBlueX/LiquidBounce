<script lang="ts">
    import Button from "../../../common/Button.svelte";
    import Chip from "../../../common/Chip.svelte";
    import Entry from "./Entry.svelte";
    import {runConfigTrackerAction} from "../../../../../integration/rest";
    import type {ConfigTrackerAction} from "../../../../../integration/types";
    import {dialog, tracker, UNKNOWN_SERVER_ICON, user} from "../marketplace";

    /**
     * The config the client runs, with what can be done about the edits made to it. Once nothing is tracked,
     * the settings from before the first load can still be restored.
     */
    let {onopen}: {
        onopen: (id: number) => void;
    } = $props();

    let busy = $state(false);

    const tracked = $derived($tracker && $tracker.state !== "None" ? $tracker : null);
    const editing = $derived(tracked?.state === "Editing");

    async function run(action: ConfigTrackerAction) {
        busy = true;
        // The client tells the player what went wrong
        const next = await runConfigTrackerAction(action).catch(() => null);
        if (next) {
            $tracker = next;
        }
        busy = false;
    }
</script>

{#snippet restoreButton()}
    <Button variant="secondary" disabled={busy} onclick={() => run("restore")}>Restore backup</Button>
{/snippet}

{#if tracked}
    <Entry address={tracked.address} image={tracked.image} fallbackImage={UNKNOWN_SERVER_ICON} highlighted
           onopen={() => onopen(tracked.id)}>
        {#snippet badges()}
            <Chip active>{editing ? "Edited" : "Tracked"}</Chip>
        {/snippet}
        {#snippet subtitle()}
            {editing ? "Your settings differ from this config" : "Your settings are those of this config"}
        {/snippet}
        {#snippet actions()}
            {#if editing && $user && tracked.editable}
                <Button disabled={busy} onclick={() => $dialog = {kind: "update"}}>Update...</Button>
            {/if}
            {#if editing}
                <Button variant="secondary" disabled={busy} onclick={() => run("revert")}>Revert</Button>
            {/if}
            {#if tracked.backup}
                {@render restoreButton()}
            {/if}
            <Button variant="secondary" disabled={busy} onclick={() => run("detach")}>Detach</Button>
        {/snippet}
    </Entry>
{:else if $tracker?.backup}
    <div class="backup">
        <span>Your settings from before the first marketplace config are kept.</span>
        {@render restoreButton()}
    </div>
{/if}

<style lang="scss">
  .backup {
    display: flex;
    align-items: center;
    justify-content: space-between;
    column-gap: 12px;
    padding: 8px 10px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    background-color: var(--clickgui-marketplace-entry-background-color);
    border-radius: 5px;
  }
</style>
