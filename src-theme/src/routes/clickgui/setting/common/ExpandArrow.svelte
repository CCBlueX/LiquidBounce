<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let expanded: boolean;

    const dispatch = createEventDispatcher();

    function handleClick() {
        expanded = !expanded;
        dispatch("click");
    }
</script>

<button
        type="button"
        class="arrow"
        class:expanded
        aria-expanded={expanded}
        aria-label={expanded ? "Collapse" : "Expand"}
        on:click={handleClick}
></button>

<style lang="scss">
  .arrow {
    flex: none;
    width: 20px;
    height: 20px;
    border: none;
    border-radius: 3px;
    background: url("/img/clickgui/icon-settings-expand.svg") center / 9px 6px no-repeat;
    opacity: .8;
    transform: rotate(-90deg);
    cursor: pointer;
    transition: ease transform .15s, ease opacity .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-control-hover-background-color);
    }

    &.expanded {
      transform: none;
    }
  }
</style>
