<script lang="ts">
    import {fly} from "svelte/transition";
</script>

<div class="menu-list" transition:fly|global={{duration: 700, x: 1000}}>
    <div class="items">
        <slot />
    </div>
</div>

<style lang="scss">
  @import "../../../../colors";

  .menu-list {
    background-color: rgba($menu-base-color, 0.68);
    flex: 1;
    border-radius: 5px;
    margin-bottom: 25px;
    position: relative;
  }

  /* TODO: scrolling doesnt have padding at top and bottom */
  .items {
    position: absolute;
    top: 0;
    left: 0;
    padding: 15px 30px;
    height: 100%;
    width: 100%;
    display: flex;
    flex-direction: column;
    overflow: auto;
    row-gap: 15px;
  }
</style>