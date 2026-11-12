<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="draggable-item" on:dragstart|preventDefault>
    <img class="handle" src="img/clickgui/icon-drag.svg" alt="Drag to reorder" draggable="false">

    <slot />
</div>

<style lang="scss">
  .draggable-item {
    display: flex;
    align-items: center;
    gap: 9px;
    min-height: 28px;
    margin-bottom: 3px;
    padding-left: 6px;
    border-radius: 3px;
    background-color: var(--clickgui-list-item-background-color);
    color: var(--clickgui-text-color);
    font-size: 12px;
    cursor: grab;

    :global(img) {
      -webkit-user-drag: none;
      user-select: none;
    }

    // The place the dragged item will drop into
    &:global(.sortable-ghost) {
      opacity: .35;
    }

    // The copy that follows the pointer sits on the body, outside every panel background
    &:global(.sortable-fallback) {
      background-color: var(--clickgui-dropdown-background-color);
      box-shadow: 0 4px 12px var(--clickgui-panel-shadow-color);
      cursor: grabbing;
    }
  }

  .handle {
    flex: none;
    width: 5px;
    height: 14px;
    opacity: .6;
  }
</style>
