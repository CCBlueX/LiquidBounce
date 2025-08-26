<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import RippleLoader from "../RippleLoader.svelte";

    export let image: string;
    export let imageText: string | null = null;
    export let imageTextBackgroundColor: string | null = null;
    export let title: string;
    export let favorite = false;

    const dispatch = createEventDispatcher();

    let previewImageLoaded = false;
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="menu-list-item" on:dblclick={() => dispatch("dblclick")}>
    <div class="image">
        {#if !previewImageLoaded}
            <div class="loader">
                <RippleLoader />
            </div>
        {/if}
        <img class="preview" on:load={() => previewImageLoaded = true} src={image} alt="preview">
        <span class="text" class:visible={imageText !== null && imageTextBackgroundColor !== null}
              style="background-color: {imageTextBackgroundColor};">{imageText}</span>
        {#if favorite}
            <img class="favorite-mark" src="img/menu/icon-favorite-mark.svg" alt="fav">
        {/if}
    </div>
    <div class="title">
        <span class="text">{title}</span>
        <slot name="tag"/>
    </div>
    <div class="subtitle">
        <slot name="subtitle"/>
    </div>
    <div class="buttons">
        <div class="active">
            <slot name="active-visible"/>
        </div>

        <slot name="always-visible"/>
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .menu-list-item {
    display: grid;
    grid-template-areas:
        "a b c"
        "a d c";
    grid-template-columns: max-content 1fr max-content;
    background-color: rgba($menu-base-color, .36);
    padding: 15px 25px;
    column-gap: 15px;
    border-radius: 5px;
    transition: ease background-color .2s;
    align-items: center;
    cursor: grab;

    &:hover {
      background-color: $accent-color;

      .subtitle {
        color: $menu-text-color;
      }

      .buttons .active {
        opacity: 1;
      }
    }
  }

  .image {
    grid-area: a;
    position: relative;

    .loader {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
    }

    .preview {
      height: 68px;
      width: 68px;
      border-radius: 50%;
      image-rendering: pixelated;
    }

    .favorite-mark {
      position: absolute;
      top: 0;
      right: 0;
    }

    .text {
      position: absolute;
      bottom: 0;
      right: 0;
      display: none;
      color: $menu-text-color;
      font-size: 12px;
      padding: 3px 10px;
      border-radius: 20px;

      &.visible {
        display: block;
      }
    }
  }

  .title {
    grid-area: b;
    align-self: flex-end;
    display: flex;
    align-items: center;

    .text {
      font-size: 20px;
      color: $menu-text-color;
      font-weight: 600;
    }
  }

  .subtitle {
    grid-area: d;
    font-size: 18px;
    color: $menu-text-dimmed-color;
    transition: ease color .2s;
    align-self: flex-start;
  }

  .buttons {
    grid-area: c;
    display: flex;

    .active {
      margin-right: 20px;
      opacity: 0;
      transition: ease opacity .2s;
    }
  }
</style>