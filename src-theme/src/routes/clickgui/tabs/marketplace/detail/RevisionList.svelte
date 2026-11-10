<script lang="ts" generics="T">
    import type {Snippet} from "svelte";

    /**
     * The newest of [items], with the older ones behind a button. [entry] renders each as a RevisionEntry,
     * whose columns the list lines up.
     */
    let {items, noun, entry}: {
        items: T[];
        noun: string;
        entry: Snippet<[T]>;
    } = $props();

    let expanded = $state(false);

    const older = $derived(items.length - 1);
</script>

<div class="revisions">
    {#each expanded ? items : items.slice(0, 1) as item, index (index)}
        {@render entry(item)}
    {/each}
</div>
{#if older > 0}
    <button type="button" class="toggle" class:expanded aria-expanded={expanded} onclick={() => expanded = !expanded}>
        {expanded ? "Hide" : "Show"} {older} older {noun}{older === 1 ? "" : "s"}
    </button>
{/if}

<style lang="scss">
  @use "../../../icon-settings-expand" as *;

  .revisions {
    display: grid;
    grid-template-columns: max-content max-content minmax(0, 1fr) max-content;
    column-gap: 12px;
  }

  .toggle {
    position: relative;
    display: block;
    margin-top: 8px;
    padding: 0 0 0 18px;
    border: none;
    background: none;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    cursor: pointer;
    transition: ease color 0.2s;

    &::after {
      @include icon-settings-expand();
      left: 0;
      right: auto;
    }

    &:hover, &:focus-visible {
      color: var(--clickgui-text-color);
    }

    &.expanded::after {
      transform: translateY(-50%) rotate(0);
      opacity: 1;
    }
  }
</style>
