<script lang="ts">
    import {fly} from "svelte/transition";
</script>

<div class="option-bar" transition:fly|global={{duration: 700, x: 1000}}>
    <slot />
</div>

<style lang="scss">
    @use "../../../../colors.scss" as *;

    .option-bar {
      background-color: rgba($menu-base-color, 0.68);
      padding: 15px 30px;
      display: flex;
      border-radius: 5px;
      align-items: center;
      column-gap: 30px;
      margin-bottom: 25px;
    }
</style>