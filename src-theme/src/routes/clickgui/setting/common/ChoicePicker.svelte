<script lang="ts">
    import {convertToSpacedString} from "../../../../theme/theme_config";
    import Dropdown from "./Dropdown.svelte";
    import Segmented from "./Segmented.svelte";

    /**
     * One of a list of options: side by side while they are few and short, a dropdown otherwise.
     */
    export let options: string[];
    export let value: string;

    const MAX_SEGMENTS = 4;
    const MAX_SEGMENT_CHARACTERS = 30;

    $: characters = options.reduce((sum, option) => sum + convertToSpacedString(option).length, 0);
    $: segmented = options.length <= MAX_SEGMENTS && characters <= MAX_SEGMENT_CHARACTERS;
</script>

{#if segmented}
    <Segmented {options} bind:value on:change/>
{:else}
    <Dropdown name={null} {options} bind:value on:change/>
{/if}
