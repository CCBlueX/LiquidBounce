<script lang="ts">
    import {onMount} from "svelte";

    const TRANSITION_DURATION = 700;

    let ready = false;

    onMount(() => {
        const timeout = window.setTimeout(() => {
            ready = true;
        }, TRANSITION_DURATION);

        return () => window.clearTimeout(timeout);
    });
</script>

{#if ready}
    <slot/>
{/if}
