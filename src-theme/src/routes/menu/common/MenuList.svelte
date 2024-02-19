<script lang="ts">
    import {fly} from "svelte/transition";
</script>

<div class="menu-list" transition:fly|global={{duration: 700, x: 1000}}>
    <slot />
</div>

<style lang="scss">
  @import "../../../colors.scss";

  .menu-list {
    background-color: rgba($menu-base-color, 0.68);
    padding: 15px 30px;
    display: flex;
    flex: 1;
    border-radius: 5px;
    margin-bottom: 25px;
  }
</style>