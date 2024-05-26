<script lang="ts">
  import { fly } from "svelte/transition";
  import { description, type TDescription } from "../clickgui_store";

  let data: TDescription | null = null;

  description.subscribe((v) => {
    data = v;
  });
</script>

{#key data}
  {#if data !== null}
    <div
      transition:fly|global={{ duration: 200, x: -15 }}
      class="description-wrapper"
      style="top: {data.y}px; left: {data.x + 20}px;"
    >
      <div class="description">
        <div class="text">{data.description}</div>
      </div>
    </div>
  {/if}
{/key}

<style lang="scss">
  @import "../../../colors.scss";

  .description-wrapper {
    position: fixed;
    z-index: 999999999999;
    transform: translateY(-50%);
  }

  .description {
    position: relative;
    border-radius: 5px;
    background-color: rgba($clickgui-base-color, 0.9);
    filter: drop-shadow(0 0 10px rgba($clickgui-base-color, 0.5));

    &::before {
      content: "";
      display: block;
      position: absolute;
      width: 0;
      height: 0;
      border-top: 8px solid transparent;
      border-bottom: 8px solid transparent;
      border-right: 8px solid rgba($clickgui-base-color, 0.9);
      left: -8px;
      top: 50%;
      transform: translateY(-50%);
    }
  }

  .text {
    font-size: 12px;
    padding: 10px;
    color: $clickgui-text-color;
  }
</style>
