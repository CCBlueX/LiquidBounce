<script lang="ts">
    import type {BindAction} from "../../../../integration/types";
    import {fly} from "svelte/transition";
    import {cubicOut} from 'svelte/easing';

    export let choices: BindAction[];
    export let chosen: typeof choices[number];
    export let onchange: () => any;

    let direction = 1;
    let arrowWrapper: HTMLSpanElement;
    let jiggle = false;

    /**
     * Switch item among {@link choices}.
     */
    function switchAction() {
        const currentIndex = choices.indexOf(chosen);
        if (currentIndex === -1) {
            throw new Error("Unexpected action: " + chosen);
        }

        const nextIndex = (currentIndex + direction) % choices.length;
        chosen = choices[nextIndex];

        triggerArrowAnimation();
        onchange();
    }

    function triggerArrowAnimation() {
        jiggle = true;
        void arrowWrapper.offsetWidth;

        setTimeout(() => {
            jiggle = false;
        }, 210);
    }
</script>

<!-- svelte-ignore a11y_consider_explicit_label -->
<button style="--direction: {direction}" on:click|stopPropagation={switchAction}>
    <span class="chosen-holder">
        {#key chosen}
            <span
                    class="chosen"
                    in:fly={{ x: direction * 5, duration: 100, delay: 100, easing: cubicOut }}
                    out:fly={{ x: -direction * 5, duration: 100, easing: cubicOut }}
            >{chosen}</span>
        {/key}
    </span>

    <span class="arrow arrow-right" class:jiggle bind:this={arrowWrapper}>
    </span>
</button>

<style lang="scss">
  @use "../../../../colors" as *;
  @use "../../icon-settings-expand" as *;

  @keyframes jiggle {
    0% { transform: translateX(0); }
    50% { transform: translateX(calc(var(--direction) * 2px)); }
    100% { transform: translateX(0); }
  }

  .arrow {
    width: 10px;

    &.arrow-right::after {
      @include icon-settings-expand($size: 10px, $right: auto);
      color: $clickgui-text-dimmed-color;
    }

    &.jiggle {
      animation: jiggle 200ms ease;
    }
  }

  .chosen-holder {
    display: grid;

    .chosen {
      font-weight: 500;
      color: $clickgui-text-color;
      font-size: 12px;
      text-overflow: ellipsis;
      white-space: nowrap;
      grid-column: 1/1;
      grid-row: 1/1;
    }
  }

  button {
    all: unset;
    background: none;
    padding: 0;
    cursor: pointer;
    display: flex;
    gap: 3px;
    align-items: center;
    position: relative;
    border: none;
  }
</style>
