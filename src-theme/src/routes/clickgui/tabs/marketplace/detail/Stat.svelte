<script lang="ts">
    import type {Snippet} from "svelte";

    let {value, label, children}: {
        value?: string;
        label: string;
        children?: Snippet;
    } = $props();
</script>

<div class="stat">
    <span class="value">
        {#if children}{@render children()}{:else}{value}{/if}
    </span>
    <span class="label">{label}</span>
</div>

<style lang="scss">
  .stat {
    display: flex;
    flex-direction: column;
    row-gap: 3px;
  }

  .value {
    font-size: 14px;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .label {
    font-size: 10px;
    font-weight: 600;
    letter-spacing: 1px;
    text-transform: uppercase;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
