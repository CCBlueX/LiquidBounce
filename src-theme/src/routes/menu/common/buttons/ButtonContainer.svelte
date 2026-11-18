<script lang="ts">
    export let vertical = false;
</script>

<div class="button-container" class:vertical>
    <slot />
</div>

<style lang="scss">

    .button-container {
      background-color: var(--menu-button-container-background-color);
      backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturation));
      border: 1px solid var(--accent-border-color);
      box-shadow: var(--elevation-sm) var(--menu-base-50-color);
      padding: 14px 24px;
      width: max-content;
      border-radius: var(--radius-md);
      display: flex;
      column-gap: var(--space-4);

      &.vertical {
        flex-direction: column;
        row-gap: var(--space-4);
        padding: 24px 14px;
      }
    }
</style>
