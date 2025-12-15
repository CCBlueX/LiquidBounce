<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let expanded: boolean;

    const dispatch = createEventDispatcher();

    function handleClick() {
        expanded = !expanded;
        dispatch("click");
    }
</script>

<!-- svelte-ignore a11y_consider_explicit_label -->
<button class="arrow" class:expanded on:click={handleClick}
></button>

<style lang="scss">
  @use "../../icon-settings-expand" as *;

  .arrow {
    width: 20px;
    position: relative;
    cursor: pointer;
    background-color: transparent;
    border: none;

    &::after {
      @include icon-settings-expand($right: 0);
    }

    &.expanded::after {
      transform: translateY(-50%) rotate(0);
      opacity: 1;
    }
  }
</style>
