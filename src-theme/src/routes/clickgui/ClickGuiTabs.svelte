<script lang="ts">
  const tabs = ["Modules", "Hud", "Settings", "Friends"];

  let activeTab = tabs[0];
</script>

<div class="tabs">
  {#each tabs as tab}
    <button
      class="tab"
      class:active={tab == activeTab}
      on:click={() => (activeTab = tab)}
    >
      {tab}
    </button>
  {/each}
</div>

<style lang="scss">
  @use "sass:color";
  @import "../../colors.scss";

  .tabs {
    position: fixed;
    display: flex;
    left: 50%;
    top: 0;
    border-radius: 0 0 20px 20px;
    transform: translateX(-50%);
    // width: 600px;

    background-color: rgba($clickgui-base-color, 0.8);
    // border-radius: 30px;

    overflow: hidden;
    // box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);

    &:focus-within {
      z-index: 9999999999;
    }
  }

  .tab {
    background-color: transparent;
    color: $clickgui-text-dimmed-color;
    border: none;
    font-size: 16px;
    font-family: inherit;
    padding: 12px 30px;

    &.active {
      background-color: color.scale($clickgui-base-color, $lightness: 15%);
      color: $clickgui-text-color;
      position: relative;
      &::after {
        content: "";
        display: block;
        position: absolute;
        right: 0;
        left: 0;
        top: -1px;

        height: 3px;
        background-color: $accent-color;
      }
    }
  }
</style>
