<script lang="ts">
    import {fly} from "svelte/transition";
    import {onMount, tick} from "svelte";
    import {portal} from "../../../integration/util";

    export let text: string;
    export let color = "var(--tooltip-background-color)";

    let element: HTMLElement;
    let shown = false;
    let x = 0;
    let y = 0;

    function updatePosition() {
        const parent = element.parentElement;
        if (!parent) return;

        const bounding = parent.getBoundingClientRect();

        x = bounding.left + bounding.width / 2;
        y = bounding.top;
    }

    async function show() {
        updatePosition();
        shown = true;
        await tick();
        updatePosition();
    }

    function hide() {
        shown = false;
    }

    onMount(() => {
        const parent = element.parentElement;
        if (!parent) return;

        parent.addEventListener("mouseenter", show);
        parent.addEventListener("mouseleave", hide);

        return () => {
            parent.removeEventListener("mouseenter", show);
            parent.removeEventListener("mouseleave", hide);
        };
    });
</script>

<div bind:this={element}>
    {#if shown}
        <div transition:fly="{{ y: -10, duration: 200 }}" class="tooltip"
             style="background-color: {color}; left: {x}px; top: {y}px;" use:portal>{text}</div>
    {/if}
</div>

<style lang="scss">

  .tooltip {
    color: var(--tooltip-text-color);
    padding: 10px 15px;
    border-radius: 20px;
    font-size: 16px;
    font-weight: 600;
    position: fixed;
    white-space: nowrap;
    transform: translate(-50%, -50px);
    z-index: 9999;

    &::after {
      content: "";
      display: block;
      height: 12px;
      width: 12px;
      background-color: inherit;
      position: absolute;
      left: 50%;
      transform: translate(-50%, 2px) rotate(45deg);
    }
  }
</style>
