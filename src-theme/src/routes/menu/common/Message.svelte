<script lang="ts">
    export let message: string;
</script>

<div class="message">
    {message}
</div>

<style lang="scss">
  @import "../../../colors.scss";

  .message {
    background-color: rgba($menu-base-color, .36);
    color: $menu-text-color;
    border-bottom: solid 1px $accent-color;
    border-radius: 5px;
    font-size: 20px;
    padding: 10px 20px;
  }
</style>