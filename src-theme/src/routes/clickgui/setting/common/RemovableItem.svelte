<script lang="ts">
    import {createEventDispatcher} from "svelte";

    const dispatch = createEventDispatcher();
</script>

<div class="removable-item">
    <slot/>

    <button type="button" class="button-remove" title="Remove" aria-label="Remove" on:click={() => dispatch("remove")}></button>
</div>

<style lang="scss">
  .removable-item {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    min-height: 28px;
    margin-bottom: 3px;
    padding: 0 4px 0 10px;
    border-radius: 3px;
    background-color: var(--clickgui-list-item-background-color);
    color: var(--clickgui-text-color);
    font-size: 12px;
  }

  // A draggable item is the row already
  :global(.draggable-item) > .removable-item {
    margin-bottom: 0;
    padding-left: 0;
    background: none;
  }

  .button-remove {
    flex: none;
    width: 20px;
    height: 20px;
    margin-left: auto;
    border: none;
    border-radius: 3px;
    background: url("/img/clickgui/icon-cross.svg") center / 9px no-repeat;
    opacity: .6;
    cursor: pointer;
    transition: ease opacity .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-control-hover-background-color);
    }
  }
</style>
