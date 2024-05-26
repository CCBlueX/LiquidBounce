<script lang="ts">
  const tabs = ["Modules", "Hud", "Settings", "Friends"];
</script>

<div class="tabs"></div>

<style lang="scss">
  @import "../../colors.scss";

  .tabs {
    position: fixed;
    left: 50%;
    top: 25px;
    transform: translateX(-50%);
    background-color: rgba($clickgui-base-color, 0.9);
    width: 600px;
    border-radius: 30px;
    overflow: hidden;
    box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);

    &:focus-within {
      z-index: 9999999999;
    }
  }
</style>
