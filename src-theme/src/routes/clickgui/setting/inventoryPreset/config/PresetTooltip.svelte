<script lang="ts">
    import {portal} from "../../../../../util/utils";
    import {scaleFactor} from "../../../clickgui_store";
    import {onDestroy, onMount} from "svelte";
    import {backOut} from "svelte/easing";
    import {scale} from "svelte/transition"

    export let text;
    export let align: "top_left" | "top_center" | "top_right" | "bottom_left" | "bottom_center" | "bottom_right" = "top_center"

    let hovered = false

    let iconRef: HTMLElement;
    let iconPosition = { top: 0, left: 0, width: 0, height: 0 };
    const resizeObserver = new ResizeObserver(updatePosition);

    function updatePosition() {
        if (!iconRef) return;
        const rect = iconRef.getBoundingClientRect();
        iconPosition = {
            top: rect.top + window.scrollY,
            left: rect.left + window.scrollX,
            width: rect.width,
            height: rect.height
        };
    }

    onMount(() => {
        window.addEventListener('resize', updatePosition);
        if (iconRef) resizeObserver.observe(iconRef);
        updatePosition();
    })

    onDestroy(() => {
        window.removeEventListener('resize', updatePosition);
        resizeObserver.disconnect();
    });

    $: {
        if (hovered) {
            updatePosition()
        }
    }
</script>

<span role="button"
      class="tooltip"
      tabindex="0"
      onmouseenter={() => hovered = true}
      onmouseleave={() => hovered = false}
>
    <img bind:this={iconRef} src="/img/clickgui/icon-question-mark.svg" alt="">
    {#if hovered}
        <span class="text {align}"
              use:portal
              transition:scale={{duration: 200, easing: backOut, start: 0.9}}
              style:--width={`${iconPosition.width}px`}
              style:--height={`${iconPosition.height}px`}
              style:--top={`${iconPosition.top}px`}
              style:--left={`${iconPosition.left}px`}
              style:--factor={`${$scaleFactor * 50}%`}
        >
            {text}
        </span>
    {/if}
</span>

<style lang="scss">
  @use "../../../../../colors.scss" as *;
  @use "sass:color";

  img {
    width: 10px;
    height: auto;
  }

  .top_left {
    transform: translateY(-100%) scale(var(--factor));
    transform-origin: bottom;
    top: calc(var(--top) - 15px);
    left: var(--left);
  }

  .top_center {
    transform: translate(-50%, -100%) scale(var(--factor));
    transform-origin: bottom;
    top: calc(var(--top) - 15px);
    left: calc(var(--left) + var(--width) / 2);
  }

  .top_right {
    transform: translate(-100%, -100%) scale(var(--factor));
    transform-origin: bottom;
    top: calc(var(--top) - 15px);
    left: calc(var(--left) + var(--width));
  }

  .bottom_left {
    transform: scale(var(--factor));
    transform-origin: top;
    top: calc(var(--top) + var(--height) + 15px);
    left: var(--left);
  }

  .bottom_center {
    transform: translateX(-50%) scale(var(--factor));
    transform-origin: top;
    top: calc(var(--top) + var(--height) + 15px);
    left: calc(var(--left) + var(--width) / 2);
  }

  .bottom_right {
    transform: translateX(-100%) scale(var(--factor));
    transform-origin: top;
    top: calc(var(--top) + var(--height) + 15px);
    left: calc(var(--left) + var(--width));
  }

  .tooltip {
    position: relative;
    margin-left: 5px;
    align-content: center;
    align-items: center;
  }

  .text {
    position: absolute;
    z-index: 99999;
    color: var(--clickgui-text-color);
    width: 150px;
    border-radius: 3px;
    padding: 2px;
    background-color: color-mix(in srgb, var(--clickgui-base-color) 85%, transparent);
    outline: 1px solid color-mix(in srgb, var(--clickgui-text-color) 15%, black);
    font-size: 12px;
  }
</style>
