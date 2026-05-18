<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {itemTextureUrl} from "../../../../integration/rest";

    const dispatch = createEventDispatcher<{
        select: { value: string }
    }>();

    export let value: string;
    export let name: string;
    export let icon: string | undefined;
    export let selected: boolean;

    let showingFallbackImage = false;

    function showFallbackIcon(event: Event) {
        const img = event.currentTarget as HTMLImageElement;

        showingFallbackImage = true;
        img.src = itemTextureUrl("minecraft:grass_block");
    }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="item" class:has-icon={icon !== undefined} class:selected
     on:click={() => dispatch("select", {value})}>
    {#if icon}
        <img class="icon" class:fallback={showingFallbackImage} src="{icon}" alt={value} on:error={showFallbackIcon}/>
    {/if}
    <div class="name">{name}</div>
</div>

<style lang="scss">
  .item {
    display: grid;
    grid-template-columns: 1fr;
    align-items: center;
    column-gap: 5px;
    cursor: pointer;
    margin: 2px 5px 2px 0;
    padding: 5px;
    border-radius: 3px;
    transition: background-color 0.2s;

    &.has-icon {
      grid-template-columns: max-content 1fr;
    }

    &.selected {
      background-color: rgba(255, 255, 255, 0.1);
    }

    &:hover {
      background-color: rgba(255, 255, 255, 0.05);
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
