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
<div class="item" class:has-icon={icon !== undefined} class:has-enabled-state={showEnabledState}
     class:pointer-cursor={pointerCursor} on:click={() => dispatch("toggle", {enabled: !enabled, value: value})}>
    {#if icon}
        <img class="icon" class:fallback={showingFallbackImage} src="{icon}" alt={value} on:error={showFallbackIcon}/>
    {/if}
    <div class="name">{name}</div>
    {#if showEnabledState}
        <div class="tick">
            {#if enabled}
                <img src="img/clickgui/icon-tick-checked.svg" alt="enabled">
            {:else}
                <img src="img/clickgui/icon-tick.svg" alt="disabled">
            {/if}
        </div>
    {/if}
</div>

<style lang="scss">

  .item {
    display: grid;
    grid-template-columns: 1fr;
    align-items: center;
    column-gap: 5px;
    margin: 2px 5px 2px 0;

    &.pointer-cursor {
      cursor: pointer;
    }

    &.has-icon:not(.has-enabled-state) {
      grid-template-columns: max-content 1fr;
    }

    &.has-icon.has-enabled-state {
      grid-template-columns: max-content 1fr max-content;
    }

    &:not(.has-icon).has-enabled-state {
      grid-template-columns: 1fr max-content;
    }
  }

  .icon {
    height: 25px;
    width: 25px;

    &.fallback {
      filter: grayscale(1);
    }
  }

  .name {
    font-size: 12px;
    color: var(--clickgui-text-color);
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
  }
</style>
