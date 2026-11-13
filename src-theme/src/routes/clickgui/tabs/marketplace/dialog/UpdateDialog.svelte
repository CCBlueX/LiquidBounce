<script lang="ts">
    import Button from "../../../common/Button.svelte";
    import Modal from "../../../common/Modal.svelte";
    import TextInput from "../../../common/TextInput.svelte";
    import {updateTrackedConfig} from "../../../../../integration/rest";
    import {tracker} from "../marketplace";

    let {onclose}: {
        onclose: () => void;
    } = $props();

    let changelog = $state("");
    let busy = $state(false);

    async function update() {
        busy = true;
        // The client tells the player what went wrong
        const next = await updateTrackedConfig(changelog.trim()).catch(() => null);
        busy = false;
        if (next) {
            $tracker = next;
            onclose();
        }
    }
</script>

<Modal title="Update {$tracker?.address ?? ''}" {onclose}>
    <p class="hint">Publishes your edits as its next revision.</p>
    <TextInput bind:value={changelog} label="What changed" multiline autofocus/>

    {#snippet footer()}
        <Button variant="secondary" onclick={onclose}>Cancel</Button>
        <Button disabled={busy} onclick={update}>Update</Button>
    {/snippet}
</Modal>

<style lang="scss">
  .hint {
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
