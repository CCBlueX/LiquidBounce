<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {fly} from "svelte/transition";
    import TitleButtonIcon from "./TitleButtonIcon.svelte";

    export let title: string;
    export let icon: string;
    export let index: number;

    let hovered = false;

    const dispatch = createEventDispatcher();
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="main-button" on:mouseenter={() => { hovered = true; dispatch("mouseenter"); }} on:mouseleave={() => hovered = false} on:click={() => hovered = false}
     on:click={() => dispatch("click")}
     in:fly|global={{duration: 180, x: -40, delay: index * 25}}>
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
    backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturation));
    width: 590px;
    padding: 22px 28px;
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    align-items: center;
    cursor: pointer;
    border: 1px solid var(--accent-border-color);
    border-radius: var(--radius-lg);
    box-shadow: var(--elevation-md) var(--menu-base-50-color);
    column-gap: var(--space-6);

    background: linear-gradient(to left, var(--menu-main-button-background-color) 50%, var(--accent-tint-color) 50%);
    background-size: 200% 100%;
    background-position: right bottom;
    will-change: background-position;
    transition: background-position var(--duration-base) var(--ease-emphasized),
    border-color var(--duration-fast) var(--ease-standard);

    &:hover {
      background-position: left bottom;
      border-color: var(--accent-border-strong-color);

      .icon {
        background-color: var(--menu-main-button-icon-hover-background-color);
        color: var(--menu-main-button-icon-hover-foreground-color);
      }
    }
  }

  .icon {
    background-color: var(--menu-main-button-icon-background-color);
    color: var(--menu-main-button-icon-foreground-color);
    width: 82px;
    height: 82px;
    border-radius: 50%;
    transition: background-color var(--duration-fast) var(--ease-standard),
    color var(--duration-fast) var(--ease-standard);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .title {
    font-size: 25px;
    color: var(--menu-main-button-text-color);
    font-weight: 600;
    letter-spacing: -0.02em;
  }
</style>
