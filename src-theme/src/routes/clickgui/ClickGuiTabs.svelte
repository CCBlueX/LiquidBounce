<script lang="ts">
  const tabs = ["Modules", "Hud", "Settings", "Friends"];

  export let activeTab: string = tabs[0];
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
    top: 14px;
    gap: 15px;
    transform: translateX(-50%);
    width: 600px;

    // border-radius: 30px;

    // overflow: hidden;
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
    flex: 1;
    border-radius: 30px;

    background-color: rgba($clickgui-base-color, 0.8);
    &.active {
      color: $clickgui-text-color;
      // border: 2px solid $accent-color;
      outline: 2px solid $accent-color;
    }
  }
</style>
