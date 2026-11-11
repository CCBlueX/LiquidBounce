<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {itemTextureUrl} from "../../../../integration/rest";

    const dispatch = createEventDispatcher<{
        toggle: { value: string, enabled: boolean }
    }>();

    export let value: string;
    export let name: string;
    export let icon: string | undefined;
    export let enabled: boolean;
    // NOTE: It would be better if enabled state handling was performed by a wrapper element.
    export let showEnabledState = true;
    export let pointerCursor = true;

    let showingFallbackImage = false;

    function showFallbackIcon(event: Event) {
        const img = event.currentTarget as HTMLImageElement;

        showingFallbackImage = true;
        img.src = itemTextureUrl("minecraft:grass_block");
    }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="item" class:enabled={showEnabledState && enabled} class:has-icon={icon !== undefined}
     class:pointer-cursor={pointerCursor} on:click={() => dispatch("toggle", {enabled: !enabled, value: value})}>
    {#if icon}
        <img class="icon" class:fallback={showingFallbackImage} src="{icon}" alt="" on:error={showFallbackIcon}/>
    {/if}
    <div class="name" title={name}>{name}</div>
    {#if showEnabledState}
        <span class="tick" role="img" aria-label={enabled ? "enabled" : "disabled"}></span>
    {/if}
</div>

<style lang="scss">
  .item {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    height: 28px;
    margin-bottom: 3px;
    padding: 0 9px 0 10px;
    border-radius: 3px;
    background-color: var(--clickgui-list-item-background-color);
    color: var(--clickgui-text-dimmed-color);
    transition: ease background-color .15s, ease color .15s;

    &.has-icon {
      padding-left: 4px;
    }

    &.pointer-cursor {
      cursor: pointer;

      &:hover {
        background-color: var(--clickgui-control-hover-background-color);
        color: var(--clickgui-text-color);
      }
    }

    &.enabled {
      color: var(--clickgui-text-color);
    }
  }

  // A removable item is the row already
  :global(.removable-item) > .item {
    flex: 1;
    margin-bottom: 0;
    padding: 0;
    background: none;
    color: var(--clickgui-text-color);
  }

  .icon {
    flex: none;
    height: 20px;
    width: 20px;
    image-rendering: pixelated;

    &.fallback {
      filter: grayscale(1);
    }
  }

  .name {
    flex: 1;
    min-width: 0;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
  }

  .tick {
    flex: none;
    width: 10px;
    height: 8px;
    background-color: var(--clickgui-text-color);
    mask: url("/img/clickgui/icon-tick.svg") center / contain no-repeat;
    opacity: .18;
    transition: ease opacity .15s, ease background-color .15s;
  }

  .enabled .tick {
    background-color: var(--accent-color);
    opacity: 1;
  }
</style>
