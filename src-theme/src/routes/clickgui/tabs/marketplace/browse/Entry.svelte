<script lang="ts">
    import type {Snippet} from "svelte";
    import Address from "../Address.svelte";

    /**
     * A row for a config or item. [address] is `author/name` or a bare name. With [onopen] the whole row opens it,
     * apart from what [actions] holds.
     */
    let {
        address,
        image,
        fallbackImage,
        onopen,
        highlighted = false,
        dimmed = false,
        badges,
        subtitle,
        meta,
        actions
    }: {
        address: string;
        image?: string;
        fallbackImage: string;
        onopen?: () => void;
        highlighted?: boolean;
        dimmed?: boolean;
        badges?: Snippet;
        subtitle?: Snippet;
        meta?: Snippet;
        actions?: Snippet;
    } = $props();

    let imageFailed = $state(false);
</script>

<div class="entry" class:highlighted class:dimmed class:openable={!!onopen}>
    <img class="image" src={imageFailed || !image ? fallbackImage : image} alt="" loading="lazy" draggable="false"
         onerror={() => imageFailed = true}/>
    <div class="text">
        <div class="heading">
            {#if onopen}
                <button type="button" class="name" onclick={onopen}><Address {address}/></button>
            {:else}
                <span class="name"><Address {address}/></span>
            {/if}
            {@render badges?.()}
        </div>
        {#if subtitle}
            <div class="subtitle">{@render subtitle()}</div>
        {/if}
    </div>
    {#if meta}
        <div class="meta">{@render meta()}</div>
    {/if}
    {#if actions}
        <div class="actions">{@render actions()}</div>
    {/if}
</div>

<style lang="scss">
  .entry {
    position: relative;
    display: flex;
    align-items: center;
    column-gap: 12px;
    min-height: 56px;
    padding: 8px 10px;
    background-color: var(--clickgui-marketplace-entry-background-color);
    border: solid 2px var(--clickgui-marketplace-entry-border-color);
    border-radius: 5px;
    transition: ease background-color 0.2s, ease border-color 0.2s;

    &.openable:hover, &:has(.name:focus-visible) {
      background-color: var(--clickgui-marketplace-entry-hover-background-color);
      border-color: var(--clickgui-marketplace-entry-hover-border-color);
    }

    &.highlighted {
      border-color: var(--clickgui-marketplace-entry-hover-border-color);
    }

    &.dimmed {
      .image, .text {
        opacity: 0.5;
      }
    }
  }

  .image {
    flex: none;
    width: 36px;
    height: 36px;
    border-radius: 3px;
    object-fit: cover;
  }

  .text {
    flex: 1;
    min-width: 0;
  }

  .heading {
    display: flex;
    align-items: center;
    column-gap: 6px;
    overflow: hidden;
  }

  .name {
    flex: none;
    max-width: 100%;
    padding: 0;
    border: none;
    background: none;
    font-family: "Inter", sans-serif;
    font-size: 14px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  // The name is the row's button, stretched over all of it
  button.name {
    cursor: pointer;

    &::after {
      content: "";
      position: absolute;
      inset: 0;
    }
  }

  .subtitle {
    margin-top: 4px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .meta {
    flex: none;
    display: flex;
    align-items: center;
    column-gap: 14px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    white-space: nowrap;
  }

  .actions {
    position: relative;
    flex: none;
    display: flex;
    align-items: center;
    column-gap: 6px;
  }
</style>
