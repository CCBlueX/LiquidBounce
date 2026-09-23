<script lang="ts">
    import type {Snippet} from "svelte";

    let {
        image,
        onclick,
        active = false,
        dim = false,
        title,
        tags,
        subtitle,
        meta,
        hover,
        primary
    }: {
        image?: string;
        onclick?: () => void;
        active?: boolean;
        dim?: boolean;
        title: Snippet;
        tags?: Snippet;
        subtitle?: Snippet;
        meta?: Snippet;
        hover?: Snippet;
        primary?: Snippet;
    } = $props();
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="row" class:active class:dim class:clickable={!!onclick} onclick={() => onclick?.()}>
    {#if image}
        <img class="image" src={image} alt=""/>
    {/if}

    <div class="main">
        <div class="head">
            <span class="title">{@render title()}</span>
            {#if tags}
                {@render tags()}
            {/if}
        </div>
        {#if subtitle}
            <div class="subtitle">{@render subtitle()}</div>
        {/if}
    </div>

    <div class="side">
        {#if meta}
            <div class="meta">{@render meta()}</div>
        {/if}
        {#if hover}
            <div class="hover">{@render hover()}</div>
        {/if}
    </div>

    {#if primary}
        <div class="primary">{@render primary()}</div>
    {/if}

    {#if onclick}
        <span class="arrow"></span>
    {/if}
</div>

<style lang="scss">
  .row {
    display: flex;
    align-items: center;
    column-gap: 12px;
    padding: 9px 14px;
    border-bottom: solid 1px var(--clickgui-global-settings-divider-color);
    border-left: solid 3px transparent;
    transition: ease background-color .2s;

    &.clickable {
      cursor: pointer;

      &:hover {
        background-color: var(--clickgui-module-hover-background-color);
      }
    }

    &.active {
      background-color: var(--clickgui-tab-active-background-color);
      border-left-color: var(--clickgui-module-settings-border-color);
    }

    &.dim :is(.image, .main, .side) {
      opacity: .5;
    }

    &:hover .hover {
      opacity: 1;
      pointer-events: auto;
    }

    &:hover .meta:has(+ .hover) {
      opacity: 0;
    }
  }

  .image {
    width: 36px;
    height: 36px;
    border-radius: 5px;
    object-fit: cover;
    image-rendering: pixelated;
    flex-shrink: 0;
  }

  .main {
    flex: 1;
    min-width: 0;
  }

  .head {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px 6px;
  }

  .title {
    font-size: 14px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    margin-right: 2px;
  }

  .subtitle {
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    margin-top: 3px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .side {
    display: grid;
    align-items: center;
    justify-items: end;

    > * {
      grid-area: 1 / 1;
    }
  }

  .meta {
    display: flex;
    align-items: center;
    column-gap: 18px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    white-space: nowrap;
    transition: ease opacity .2s;
  }

  .hover {
    display: flex;
    gap: 6px;
    opacity: 0;
    pointer-events: none;
    transition: ease opacity .2s;
  }

  .primary {
    display: flex;
    gap: 6px;
  }

  .arrow {
    width: 11px;
    height: 11px;
    flex-shrink: 0;
    background-image: url("/img/clickgui/icon-settings-expand.svg");
    background-position: center;
    background-repeat: no-repeat;
    opacity: .5;
    transform: rotate(-90deg);
  }
</style>
