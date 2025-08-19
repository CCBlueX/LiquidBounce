<script lang="ts">
    import {listen} from "../../integration/ws";
    import type {InventoryManagerProgressEvent} from "../../integration/events";
    import {fade} from 'svelte/transition';

    let count = $state(0);
    let remaining = $state(0);

    listen("inventoryManagerProgress", (event: InventoryManagerProgressEvent) => {
        count = event.count;
        remaining = event.remaining;
    });

    const progressPercentage = $derived(
        count > 0 ? Math.max(0, Math.min(100, (count - remaining) / count * 100)) : 0
    );
</script>

{#if count}
    <div class="progress-container" transition:fade={{duration: 400}}>
        <div class="progress-bar" style="width: {progressPercentage}%"></div>
        <div class="progress-text">
            {Math.round(progressPercentage)}% ({count - remaining}/{count})
        </div>
    </div>
{/if}

<style lang="scss">
  @use "../../colors.scss" as *;

  .progress-container {
    font-family: monospace;
    position: fixed;
    top: 15vh;
    left: 50%;
    transform: translateX(-50%);
    width: 80%;
    max-width: 800px;
    height: 16px;
    background-color: #f0f0f0;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1) inset;
    overflow: hidden;

    .progress-bar {
      height: 100%;
      background: $accent-color;
      transition: width 0.4s ease-in-out;
      border-radius: 4px;
    }

    .progress-text {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      font-size: 12px;
      font-weight: 500;
      color: #333;
      text-shadow: 0 0 2px rgba(255,255,255,0.8);
    }
  }
</style>
