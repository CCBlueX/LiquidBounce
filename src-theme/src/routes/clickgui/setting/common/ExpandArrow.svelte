<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let expanded: boolean;
    export let expandable: boolean = true;
    export let compact = false;
    export let dimmed = false;

    const dispatch = createEventDispatcher();

    function handleClick() {
        if (expandable) {
            expanded = !expanded;
            dispatch("click");
        }
    }
</script>

<!-- svelte-ignore a11y_consider_explicit_label -->
<button class="arrow" class:expanded class:compact class:dimmed on:click={handleClick}
></button>

<style lang="scss">
  @use "../../../../colors" as *;

  .arrow {
    width: 20px;
    position: relative;
    cursor: pointer;
    background-color: transparent;
    border: none;

    &.compact {
      width: 10px;
    }

    &.dimmed {
      color: $clickgui-text-dimmed-color;
    }

    &::after {
      content: "";
      display: block;
      position: absolute;
      height: 10px;
      width: 10px;
      right: 0;
      top: 50%;
      background-image: url("/img/clickgui/icon-settings-expand.svg");
      background-position: center;
      background-repeat: no-repeat;
      transform-origin: 50% 50%;
      transform: translateY(-50%) rotate(-90deg);
      transition: ease opacity 0.2s,
      ease transform 0.4s;
    }

    &::after.compact {
      right: auto;
    }

    &.expanded::after {
      transform: translateY(-50%) rotate(0);
      opacity: 1;
    }
  }
</style>
