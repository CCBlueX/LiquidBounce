<script lang="ts">
    export let vertical = false;
</script>

<div class="button-container" class:vertical>
    <slot />
</div>

<style lang="scss">

    .button-container {
      background-color: var(--menu-button-container-background-color);
      padding: 15px 30px;
      width: max-content;
      border-radius: 5px;
      display: flex;
      column-gap: 20px;

      &.vertical {
        flex-direction: column;
        row-gap: 20px;
        padding: 30px 15px;
      }
    }
</style>
