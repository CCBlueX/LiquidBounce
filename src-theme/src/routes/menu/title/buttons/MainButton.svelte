<script lang="ts">
    import {fly} from "svelte/transition";
    import {createEventDispatcher} from "svelte";
    import {backIn, backOut} from "svelte/easing";
    import TitleButtonIcon from "./TitleButtonIcon.svelte";

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
        <TitleButtonIcon {icon} />
    </div>

    <div class="title">{title}</div>

    <div class="wrapped-content">
        <slot parentHovered={hovered}/>
    </div>
</div>

<style lang="scss">

  .main-button {
    background-color: var(--menu-main-button-background-color);
    width: 590px;
    padding: 25px 35px;
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    align-items: center;
    cursor: pointer;
    border-radius: 5px;
    column-gap: 25px;

    background: linear-gradient(to left, var(--menu-main-button-background-color) 50%, var(--menu-main-button-accent-color) 50%);
    background-size: 200% 100%;
    background-position: right bottom;
    will-change: background-position;
    transition: background-position .2s ease-out;

    &:hover {
      background-position: left bottom;

      .icon {
        background-color: var(--menu-main-button-icon-hover-background-color);
        color: var(--menu-main-button-icon-hover-foreground-color);
      }
    }
  }

  .icon {
    background-color: var(--menu-main-button-icon-background-color);
    color: var(--menu-main-button-icon-foreground-color);
    width: 90px;
    height: 90px;
    border-radius: 50%;
    transition: ease background-color 0.2s, ease color 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .title {
    font-size: 26px;
    color: var(--menu-main-button-text-color);
    font-weight: 600;
  }
</style>
