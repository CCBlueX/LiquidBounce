<script lang="ts">
    import {createEventDispatcher} from "svelte";

    const dispatch = createEventDispatcher();
</script>

<div class="actions">
    <button type="button" title="Select all" aria-label="Select all" on:click|stopPropagation={() => dispatch("selectAll")}>
  <span class="icon icon-select"></span>
    </button>
    <button type="button" title="Deselect all" aria-label="Deselect all" on:click|stopPropagation={() => dispatch("deselectAll")}>
  <span class="icon icon-deselect"></span>
    </button>
</div>

<style lang="scss">
  .actions {
    display: flex;
    align-items: center;
    gap: 3px;
  }

  button {
    width: 18px;
    height: 18px;
    display: grid;
    place-items: center;
    padding: 0;
    border: none;
    border-radius: 3px;
    background-color: transparent;
    cursor: pointer;
    opacity: 0.55;
    transition: ease background-color 0.15s, ease opacity 0.15s;

    &:hover {
      background-color: var(--clickgui-selection-chip-background-color);
      opacity: 1;
    }
  }

  .icon {
    width: 10px;
    height: 10px;
    position: relative;
    display: block;
    color: var(--clickgui-text-color);
  }

  .icon-select::after {
    content: "";
    position: absolute;
    width: 7px;
    height: 4px;
    left: 1px;
    top: 1px;
    border-left: 2px solid currentColor;
    border-bottom: 2px solid currentColor;
    transform: rotate(-45deg);
  }

  .icon-deselect::before,
  .icon-deselect::after {
    content: "";
    position: absolute;
    width: 11px;
    height: 2px;
    left: 0;
    top: 4px;
    border-radius: 1px;
    background-color: currentColor;
  }

  .icon-deselect::before {
    transform: rotate(45deg);
  }

  .icon-deselect::after {
    transform: rotate(-45deg);
  }
</style>