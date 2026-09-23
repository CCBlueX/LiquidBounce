<script lang="ts">
    import Menu from "./Menu.svelte";
    import Address from "./Address.svelte";
    import type {ConfigTracker} from "../../../../integration/types";
    import type {MenuEntry} from "./marketplace";

    let {tracker, loggedIn, online = true, onchange, onpublish, onupdate, onopen}: {
        tracker: ConfigTracker;
        loggedIn: boolean;
        online?: boolean;
        onchange: (action: "revert" | "restore" | "detach") => void;
        onpublish: () => void;
        onupdate: () => void;
        onopen: () => void;
    } = $props();

    const entries = $derived.by(() => {
        const list: MenuEntry[] = [];
        const editing = tracker.state === "Editing";

        if (editing && online && loggedIn && tracker.own) {
            list.push({title: "Update...", onclick: onupdate});
        }
        if (editing && online && loggedIn) {
            list.push({title: "Publish...", onclick: onpublish});
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

<Menu {entries} active={tracker.state !== "None"}>
    {#snippet trigger()}
        {#if tracker.state === "None"}
            <span>Settings backup</span>
        {:else}
            <span class="state">{tracker.state === "Editing" ? "Edited" : "Tracked"}</span>
            <span><Address address={tracker.address}/>{tracker.state === "Editing" ? "*" : ""}</span>
        {/if}
    {/snippet}
</Menu>

<style lang="scss">
  .state {
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
