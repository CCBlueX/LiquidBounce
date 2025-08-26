<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import {fade} from "svelte/transition";
    import type {ProgressEvent} from "../../../../integration/events";
    import {WindowSize} from "../../../../util/WindowSize";
    import {onMount} from "svelte";

    let progressEvent: ProgressEvent | null = null;
    const {width, destroy} = WindowSize();
    let progressWidth = 0;

    $: progressWidth = $width * 0.4;

    listen("progress", (e: ProgressEvent) => {
        progressEvent = e;
        // Auto-hide after 3 seconds if progress completes
        if (e.progress >= e.maxProgress) {
            setTimeout(() => {
                progressEvent = null;
            }, 100);
        }
    });

    onMount(() => {
        return destroy;
    });
</script>

{#if progressEvent}
    <div
            class="progress-container"
            style="width: {progressWidth}px;"
            transition:fade={{ duration: 200 }}
    >
        <div class="progress-header">
            <span class="progress-title">{progressEvent.title}</span>
            <span
                    class="progress-percent"
                    style="--progress-percent: {Math.round((progressEvent.progress / progressEvent.maxProgress) * 100)}%"
            >
                {Math.round((progressEvent.progress / progressEvent.maxProgress) * 100)}%
            </span>
        </div>
        <div class="progress-bar">
            <div
                    class="progress-fill"
                    style="width: {(progressEvent.progress / progressEvent.maxProgress) * 100}%;">

            </div>
        </div>
    </div>
{/if}

<style lang="scss">
  @import "../../../../colors.scss";

  .progress-container {
    position: fixed;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%) scale(1);
    height: 50px;
    min-height: 50px;
    background: color-mix(
                    in srgb,
                    rgba(20, 20, 20, 0.5) 0%,
                    rgba(darken($base, 5%), 0.5) 100%
    );
    border-radius: 14px;
    padding: 12px 16px;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.03) inset;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    z-index: 1000;
    transition: all 0.3s ease;
  }

  .progress-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }

  .progress-title {
    font-size: 14px;
    font-weight: 600;
    color: $text;
  }

  .progress-percent {
    font-size: 14px;
    font-weight: 600;
    color: color-mix(
                    in srgb,
                    var(--primary-color) calc(var(--progress-percent) * 0.8),
                    $text 20%
    );
    transition: color 0.3s ease;
  }

  .progress-bar {
    position: relative;
    height: 8px;
    flex-shrink: 0;
    width: 100%;
    background: color-mix(in srgb, rgba(20, 20, 20) 30%, transparent);
    border-radius: 4px;
    overflow: visible;
    box-shadow: 0 0 8px rgba(black, 0.2),
    0 0 12px rgba(black, 0.3),
    0 0 20px rgba(black, 0.3),
    0 0 24px rgba(black, 0.2);
  }

  .progress-fill {
    position: relative;
    height: 8px !important;
    border-radius: 4px;
    background: linear-gradient(
                    135deg in oklch,
                    color-mix(in srgb, var(--primary-color) 80%, transparent) 10%,
                    color-mix(in srgb, var(--secondary-color) 80%, transparent) 90%
    );
    transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  }

  .progress-fill::before {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
    box-shadow: 0 0 4px color-mix(in srgb, var(--primary-color) 20%, transparent),
    0 0 6px color-mix(in srgb, var(--primary-color) 30%, transparent),
    0 0 10px color-mix(in srgb, var(--secondary-color) 30%, transparent),
    0 0 14px color-mix(in srgb, var(--secondary-color) 20%, transparent);
    z-index: -1;
  }
</style>
