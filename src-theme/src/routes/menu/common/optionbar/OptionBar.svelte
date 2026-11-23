<script lang="ts">
    import {fly} from "svelte/transition";
</script>

<div class="option-bar" in:fly|global={{duration: 180, x: 40}}>
    <slot />
</div>

<style lang="scss">

    .option-bar {
      background-color: var(--menu-button-container-background-color);
      padding: 15px 30px;
      display: flex;
      border-radius: 5px;
      align-items: center;
      column-gap: 30px;
      margin-bottom: 25px;
    }
</style>
