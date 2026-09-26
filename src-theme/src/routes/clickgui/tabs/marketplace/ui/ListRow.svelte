<script lang="ts">
    import type {Snippet} from "svelte";
    import Badge from "./Badge.svelte";
    import Address from "../Address.svelte";

    /**
     * The title is [address] with its author dimmed, or [title], followed by [by]. [hover] replaces [meta] while
     * the row is hovered. A [large] row heads a page.
     */
    let {
        large = false,
        image,
        onclick,
        active = false,
        dim = false,
        title,
        address,
        by,
        badges = [],
        subtitle,
        meta,
        hover,
        actions
    }: {
        large?: boolean;
        image?: string;
        onclick?: () => void;
        active?: boolean;
        dim?: boolean;
        title?: string;
        address?: string;
        by?: string;
        badges?: string[];
        subtitle?: string;
        meta?: Snippet;
        hover?: Snippet;
        actions?: Snippet;
    } = $props();
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="row" class:large class:active class:dim class:clickable={!!onclick} onclick={() => onclick?.()}>
    {#if image}
        <img class="image" src={image} alt=""/>
    {/if}

    <div class="main">
        <div class="head">
            <span class="title">
                {#if address}<Address {address}/>{:else}{title}{/if}{#if by}<span class="by">by {by}</span>{/if}
            </span>
            {#each badges as badge}
                <Badge text={badge}/>
            {/each}
        </div>
        {#if subtitle !== undefined}
            <div class="subtitle">{subtitle}</div>
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

    {#if actions}
        <div class="actions">{@render actions()}</div>
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

    &.large {
      flex: 1;
      min-width: 0;
      padding: 0;
      border: none;

      .image {
        width: 40px;
        height: 40px;
      }

      .title {
        font-size: 16px;
        margin-right: 4px;
      }

      .by {
        margin-left: 6px;
        font-size: 13px;
      }

      .subtitle {
        white-space: normal;
      }
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

  .actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .by {
    margin-left: 5px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
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
