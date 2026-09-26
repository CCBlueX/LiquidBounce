<script lang="ts">
    import ActionMenu from "../../../common/ActionMenu.svelte";
    import Address from "../../../common/Address.svelte";
    import type {ConfigTracker} from "../../../../../integration/types";
    import {dialog, trackingName} from "../marketplace";
    import type {MenuEntry} from "../../../common/ActionMenu.svelte";

    let {tracker, loggedIn, online = true, onchange, onopen}: {
        tracker: ConfigTracker;
        loggedIn: boolean;
        online?: boolean;
        onchange: (action: "revert" | "restore" | "detach") => void;
        onopen: () => void;
    } = $props();

    const entries = $derived.by(() => {
        const list: MenuEntry[] = [];
        const editing = tracker.state === "Editing";

        if (editing && online && loggedIn && tracker.own) {
            list.push({title: "Update...", onclick: () => dialog.set({kind: "update"})});
        }
        if (editing && online && loggedIn) {
            list.push({title: "Publish...", onclick: () => dialog.set({kind: "publish"})});
        }
        if (editing) {
            list.push({title: "Revert", onclick: () => onchange("revert")});
        }
        if (tracker.backup) {
            list.push({title: "Restore", hint: "Your settings from before", onclick: () => onchange("restore")});
        }
        if (tracker.state !== "None") {
            list.push({title: "Detach", hint: "Keep settings, stop tracking", onclick: () => onchange("detach")});
        }
        if (tracker.state !== "None" && online) {
            list.push({title: "Details", onclick: onopen});
        }
        return list;
    });
</script>

<ActionMenu {entries} active={tracker.state !== "None"}>
    {#snippet trigger()}
        {#if tracker.state === "None"}
            <span>Settings backup</span>
        {:else}
            <span class="state">{trackingName(tracker.state)}</span>
            <span><Address address={tracker.address}/>{tracker.state === "Editing" ? "*" : ""}</span>
        {/if}
    {/snippet}
</ActionMenu>

<style lang="scss">
  .state {
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
