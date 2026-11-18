<script lang="ts">
    import {fly} from "svelte/transition";
</script>

<div class="option-bar" in:fly|global={{duration: 180, x: 40}}>
    <slot />
</div>

<style lang="scss">

    .option-bar {
      background-color: var(--menu-button-container-background-color);
      backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturation));
      padding: 14px 24px;
      display: flex;
      border: 1px solid var(--accent-border-color);
      border-radius: var(--radius-md);
      box-shadow: var(--elevation-sm) var(--menu-base-50-color);
      align-items: center;
      column-gap: var(--space-6);
      margin-bottom: 25px;
    }
</style>
