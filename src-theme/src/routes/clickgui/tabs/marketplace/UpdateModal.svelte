<script lang="ts">
    import Dialog from "./Dialog.svelte";
    import Button from "./Button.svelte";
    import Label from "./Label.svelte";
    import Input from "./Input.svelte";
    import {updateTrackedConfig} from "../../../../integration/rest";
    import type {ConfigTracker} from "../../../../integration/types";
    import {attempt, notify} from "./marketplace";

    let {open = $bindable(), tracker}: {
        open: boolean;
        tracker: ConfigTracker | null;
    } = $props();

    let changelog = $state("");
    let loading = $state(false);

    async function update() {
        if (loading) {
            return;
        }

        loading = true;
        const result = await attempt(() => updateTrackedConfig(changelog.trim()));
        loading = false;

        if (result) {
            open = false;
            changelog = "";
            notify(`Updated ${result.address}`);
        }
    }
</script>

<Dialog bind:open title="Update {tracker?.address ?? ''}" width={460}>
    <Label text="Changelog"/>
    <Input bind:value={changelog} multiline/>

    {#snippet footer()}
        <Button title="Cancel" onclick={() => open = false}/>
        <Button title="Update" primary disabled={loading} onclick={update}/>
    {/snippet}
</Dialog>
