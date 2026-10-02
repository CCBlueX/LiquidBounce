<script lang="ts">
    import PillButton from "./PillButton.svelte";

    let {options, value, onchange}: {
        options: string[];
        value: string;
        onchange: (value: string) => void;
    } = $props();
</script>

<div class="segmented">
    {#each options as option (option)}
        <PillButton title={option} segment active={option === value} onclick={() => onchange(option)}/>
    {/each}
</div>

<style lang="scss">
  .segmented {
    flex-shrink: 0;
    display: flex;
    gap: 3px;
    padding: 4px;
    border-radius: 999px;
    background-color: var(--clickgui-tabs-background-color);
    box-shadow: 0 0 10px var(--clickgui-tabs-shadow-color);
  }
</style>
