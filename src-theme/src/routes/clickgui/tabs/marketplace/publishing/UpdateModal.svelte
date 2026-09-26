<script lang="ts">
    import Dialog from "../ui/Dialog.svelte";
    import PillButton from "../ui/PillButton.svelte";
    import SectionLabel from "../ui/SectionLabel.svelte";
    import TextField from "../ui/TextField.svelte";
    import {updateTrackedConfig} from "../../../../../integration/rest";
    import type {ConfigTracker} from "../../../../../integration/types";
    import {attempt, notify} from "../marketplace";

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
    <SectionLabel text="Changelog"/>
    <TextField bind:value={changelog} multiline/>

    {#snippet footer()}
        <PillButton title="Cancel" onclick={() => open = false}/>
        <PillButton title="Update" primary disabled={loading} onclick={update}/>
    {/snippet}
</Dialog>
