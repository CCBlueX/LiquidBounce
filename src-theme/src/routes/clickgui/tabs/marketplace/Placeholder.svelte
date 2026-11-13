<script lang="ts">
    import type {Snippet} from "svelte";

    let {title, text, actions}: {
        title: string;
        text?: string;
        actions?: Snippet;
    } = $props();
</script>

<div class="placeholder">
    <div class="title">{title}</div>
    {#if text}
        <div class="text">{text}</div>
    {/if}
    {#if actions}
        <div class="actions">{@render actions()}</div>
    {/if}
</div>

<style lang="scss">
  .placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    row-gap: 8px;
    padding: 40px 20px;
    text-align: center;
  }

  .title {
    font-size: 14px;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .text {
    max-width: 420px;
    font-size: 12px;
    font-weight: 500;
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }

  .actions {
    display: flex;
    column-gap: 6px;
    margin-top: 4px;
  }
</style>
