<script lang="ts">
    export let image: string;
    export let imageText: string | null = null;
    export let imageTextBackgroundColor: string | null = null;
    export let title: string;
    export let titleTag: string | null = null;
    export let subtitle: string;

</script>

<div class="menu-list-item">
    <div class="image">
        <img src={image} alt="preview">
        <span class="text" class:visible={imageText !== null && imageTextBackgroundColor !== null} style="background-color: {imageTextBackgroundColor};">{imageText}</span>
    </div>
    <div class="title">
        <span class="text">{title}</span>
        <span class="tag" class:visible={titleTag !== null}>{titleTag}</span>
    </div>
    <div class="subtitle">{subtitle}</div>
    <div class="buttons">
        <div class="active">
            <slot name="active-visible"/>
        </div>

        <slot name="always-visible"/>
    </div>
</div>

<style lang="scss">
  @import "../../../../colors.scss";

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
    cursor: pointer;
    transition: ease background-color .2s;
    align-items: center;

    &:hover {
      background-color: $accent-color;

      .subtitle {
        color: $menu-text-color;
      }

      .title .tag {
        background-color: $menu-text-color;
        color: $accent-color;
      }

      .buttons .active {
        opacity: 1;
      }
    }
  }

  .image {
    grid-area: a;
    position: relative;

    img {
      height: 68px;
      width: 68px;
      border-radius: 50%;
      image-rendering: pixelated;
    }

    .text {
      position: absolute;
      top: 0;
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

    .tag {
      background-color: $accent-color;
      color: $menu-text-color;
      font-size: 12px;
      padding: 3px 10px;
      border-radius: 20px;
      margin-left: 10px;
      display: none;
      transition: ease color .2s, ease background-color .2s;

      &.visible {
        display: block;
      }
    }
  }

  .subtitle {
    grid-area: d;
    font-size: 18px;
    color: $menu-text-dimmed-color;
    transition: ease color .2s;
  }

  .buttons {
    grid-area: c;
    display: flex;

    .active {
      opacity: 0;
      transition: ease opacity .2s;
    }
  }
</style>