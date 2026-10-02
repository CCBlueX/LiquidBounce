<script lang="ts">
    import type {Snippet} from "svelte";

    let {title, children, actions}: {
        title: string;
        children?: Snippet;
        actions?: Snippet;
    } = $props();
</script>

<div class="message">
    <span class="title">{title}</span>
    {#if children}
        <span class="text">{@render children()}</span>
    {/if}
    {#if actions}
        <div class="actions">{@render actions()}</div>
    {/if}
</div>

<style lang="scss">
  .message {
    display: flex;
    flex-direction: column;
    align-items: center;
    row-gap: 8px;
    padding: 36px 20px;
    text-align: center;
  }

  .title {
    font-size: 15px;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .text {
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    max-width: 520px;
    line-height: 1.5;
  }

  .actions {
    display: flex;
    gap: 8px;
    margin-top: 6px;
  }
</style>
