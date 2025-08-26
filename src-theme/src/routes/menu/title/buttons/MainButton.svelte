<script lang="ts">
    import {fade, fly} from "svelte/transition";
    import {createEventDispatcher} from "svelte";
    import {backIn, backOut} from "svelte/easing";

    export let title: string;
    export let icon: string;
    export let index: number;

    let hovered = false;

    const dispatch = createEventDispatcher();
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="main-button" on:mouseenter={() => hovered = true} on:mouseleave={() => hovered = false} on:click={() => hovered = false}
     on:click={() => dispatch("click")} out:fly|global={{duration: 400, x: -500, delay: index * 100, easing: backIn}}
     in:fly|global={{duration: 400, x: -500, delay: index * 100, easing: backOut}}>
    <div class="icon">
        {#if !hovered}
            <img transition:fade={{duration: 200}} src="img/menu/icon-{icon}.svg" alt={icon}>
        {:else}
            <img transition:fade={{duration: 200}} src="img/menu/icon-{icon}-hover.svg" alt={icon}>
        {/if}
    </div>

    <div class="title">{title}</div>

    <div class="wrapped-content">
        <slot parentHovered={hovered}/>
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .main-button {
    background-color: rgba($menu-base-color, 0.68);
    width: 590px;
    padding: 25px 35px;
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    align-items: center;
    cursor: pointer;
    border-radius: 5px;
    column-gap: 25px;

    background: linear-gradient(to left, rgba($menu-base-color, .68) 50%, $accent-color 50%);
    background-size: 200% 100%;
    background-position: right bottom;
    will-change: background-position;
    transition: background-position .2s ease-out;

    &:hover {
      background-position: left bottom;

      .icon {
        background-color: $menu-text-color;
      }
    }
  }

  .icon {
    background-color: $accent-color;
    width: 90px;
    height: 90px;
    border-radius: 50%;
    transition: ease background-color 0.2s;
    position: relative;

    img {
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
    }
  }

  .title {
    font-size: 26px;
    color: $menu-text-color;
    font-weight: 600;
  }
</style>
