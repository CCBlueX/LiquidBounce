<script lang="ts">
    import type {Snippet} from "svelte";

    /**
     * A line of a config's history or an item's versions, inside a RevisionList.
     */
    let {title, text = "", highlighted = false, dimmed = false, badges, meta}: {
        title: string;
        text?: string;
        highlighted?: boolean;
        dimmed?: boolean;
        badges?: Snippet;
        meta?: Snippet;
    } = $props();
</script>

<div class="revision" class:highlighted class:dimmed>
    <span class="title">{title}</span>
    <span class="badges">{@render badges?.()}</span>
    <span class="text">{text}</span>
    <span class="meta">{@render meta?.()}</span>
</div>

<style lang="scss">
  .revision {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: subgrid;
    align-items: center;
    min-height: 34px;
    padding: 6px 10px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    border-left: solid 2px transparent;

    &:not(:last-child) {
      border-bottom: solid 1px var(--clickgui-marketplace-divider-color);
    }

    &.highlighted {
      border-left-color: var(--clickgui-marketplace-entry-hover-border-color);
      background-color: var(--clickgui-marketplace-entry-hover-background-color);
    }

    &.dimmed {
      opacity: 0.5;
    }
  }

  .title {
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .badges {
    display: flex;
    column-gap: 6px;
  }

  .text {
    line-height: 1.5;
    white-space: pre-line;
    overflow-wrap: anywhere;
    user-select: text;
  }

  .meta {
    text-align: right;
    white-space: nowrap;
  }
</style>
