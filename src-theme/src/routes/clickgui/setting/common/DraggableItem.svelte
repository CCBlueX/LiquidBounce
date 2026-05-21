<script>
</script>

<div class="draggable-item">
    <img src="img/clickgui/icon-drag.svg" alt="drag">

    <slot />
</div>

<style lang="scss">
  .draggable-item {
    display: grid;
    grid-template-columns: max-content 1fr;
    column-gap: 10px;
    align-items: center;
    cursor: pointer;
  }
</style>
