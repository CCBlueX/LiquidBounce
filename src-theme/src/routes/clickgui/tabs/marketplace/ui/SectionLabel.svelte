<script lang="ts">
    import type {Snippet} from "svelte";

    let {text, children}: { text: string; children?: Snippet } = $props();
</script>

<div class="label">
    <span>{text}</span>
    {#if children}
        <span class="hint">{@render children()}</span>
    {/if}
</div>

<style lang="scss">
  .label {
    display: flex;
    align-items: baseline;
    column-gap: 8px;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 1px;
    text-transform: uppercase;
    color: var(--clickgui-text-dimmed-color);
    padding: 14px 0 6px;
  }

  .hint {
    font-weight: 500;
    letter-spacing: normal;
    text-transform: none;
    opacity: .7;
  }
</style>
