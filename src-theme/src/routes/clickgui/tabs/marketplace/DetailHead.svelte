<script lang="ts">
    import type {Snippet} from "svelte";

    let {image, onback, title, tags, subtitle, actions}: {
        image?: string;
        onback: () => void;
        title: Snippet;
        tags?: Snippet;
        subtitle?: Snippet;
        actions?: Snippet;
    } = $props();
</script>

<div class="head">
    <button type="button" class="back" aria-label="Back" onclick={onback}></button>
    {#if image}
        <img class="image" src={image} alt=""/>
    {/if}
    <div class="main">
        <div class="line">
            <span class="title">{@render title()}</span>
            {#if tags}
                {@render tags()}
            {/if}
        </div>
        {#if subtitle}
            <div class="subtitle">{@render subtitle()}</div>
        {/if}
    </div>
    {#if actions}
        <div class="actions">{@render actions()}</div>
    {/if}
</div>

<style lang="scss">
  .head {
    display: flex;
    align-items: center;
    column-gap: 12px;
    background-color: var(--clickgui-window-header-background-color);
    border-bottom: solid 2px var(--clickgui-window-header-border-color);
    padding: 12px 18px;
  }

  .back {
    width: 24px;
    height: 24px;
    border: none;
    border-radius: 999px;
    cursor: pointer;
    background: url("/img/clickgui/icon-settings-expand.svg") center / 11px no-repeat;
    transform: rotate(90deg);
    opacity: .7;
    transition: ease opacity .2s, ease background-color .2s;

    &:hover {
      opacity: 1;
      background-color: var(--clickgui-tab-hover-background-color);
    }
  }

  .image {
    width: 40px;
    height: 40px;
    border-radius: 5px;
    object-fit: cover;
    image-rendering: pixelated;
  }

  .main {
    flex: 1;
    min-width: 0;
  }

  .line {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px 6px;
  }

  .title {
    font-size: 16px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    margin-right: 4px;
  }

  .subtitle {
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    margin-top: 3px;
  }

  .actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }
</style>
