<script lang="ts">
    import ToolTip from "../ToolTip.svelte";
    import {createEventDispatcher} from "svelte";

    export let title: string;
    export let icon: string;

    const dispatch = createEventDispatcher();
</script>

<button class="button" type="button" on:click={() => dispatch("click")}>
    <ToolTip text={title} color="black"/>
    <img class="icon" src="img/menu/icon-{icon}.svg" alt={icon}>
</button>

<style lang="scss">
  .button {
    display: grid;
    place-items: center;
    min-height: 36px;
    min-width: 36px;
    padding: 4px;
    background-color: transparent;
    border: 1px solid transparent;
    border-radius: var(--radius-sm);
    cursor: pointer;
    position: relative;
    margin-left: 8px;
    transition: background-color var(--duration-fast) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard);

    &:hover {
      background-color: var(--accent-subtle-background-color);
      border-color: var(--accent-border-color);
    }
  }

  .icon {
    height: 25px;
    width: 25px;
  }
</style>