<script lang="ts">

    export let message: string;
    export let severity: string;
    export let remaining: number = 3;

    $: subtitle =
        severity === 'ENABLED' ? `${message} has been enabled! (${remaining.toFixed(1)}s)` :
            severity === 'DISABLED' ? `${message} has been disabled! (${remaining.toFixed(1)}s)` :
                message;

</script>

<div class="notification {severity.toLowerCase()}">
    <div class="icon-container">
        {#if ['ENABLED', 'DISABLED'].includes(severity)}
            <div class="icon {severity.toLowerCase()}">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                    <path class="toggle-track"
                          d="M17 6H7C3.69 6 1 8.69 1 12S3.69 18 7 18H17C20.31 18 23 15.31 23 12S20.31 6 17 6M17 16H7C4.79 16 3 14.21 3 12S4.79 8 7 8H17C19.21 8 21 9.79 21 12S19.21 16 17 16Z"/>
                    <path class="toggle-slider"
                          d="M17 9C15.34 9 14 10.34 14 12S15.34 15 17 15 20 13.66 20 12 18.66 9 17 9Z"/>
                </svg>
            </div>
        {:else if severity === 'BLINKING' || severity === 'BLINKED'}
            <div class="icon {severity.toLowerCase()}">
                <div class="svg-container">
                    <svg
                            xmlns="http://www.w3.org/2000/svg"
                            viewBox="0 0 24 24"
                            fill="none"
                    >
                        <path d="M21 12C21 16.9706 16.9706 21 12 21C9.69494 21 7.59227 20.1334 6 18.7083L3 16M3 12C3 7.02944 7.02944 3 12 3C14.3051 3 16.4077 3.86656 18 5.29168L21 8M3 21V16M3 16H8M21 3V8M21 8H16"
                              stroke="currentColor"
                              stroke-width="2"
                              stroke-linecap="round"
                              stroke-linejoin="round"/>
                    </svg>
                </div>
            </div>
        {:else}
            <div class="icon {severity.toLowerCase()}"></div>
        {/if}
    </div>
    <div class="content">
        <h3 class="title">{severity}</h3>
        <p class="message">{subtitle}</p>
        {#if ['ENABLED', 'DISABLED', 'SUCCESS', 'ERROR', 'INFO', 'BLINKING', 'BLINKED'].includes(severity)}
            <div class="progress-container">
                <!-- svelte-ignore element_invalid_self_closing_tag -->
                <div
                        class="progress-bar {severity.toLowerCase()}"
                        style="width: {(remaining / 3) * 100}%;"
                />
            </div>
        {/if}
    </div>
</div>

<style lang="scss">
  @use "sass:color";
    @use "../../../../colors" as *;


  .notification {
    --accent-color: #{var(--primary-color)};
    --success-color: #{color.mix($green, $text, 70%)};
    --error-color: #{color.mix($red, $text, 70%)};
    --info-color: #{color.mix($blue, $text, 70%)};
    display: flex;
    align-items: center;
    gap: 12px;
    background: linear-gradient(
                    135deg,
                    rgba(20, 20, 20, 0.5) 0%,
                    rgba(color.adjust($base, $lightness: -5%), 0.5) 100%
    );
    border-radius: var(--border-radius);
    width: 400px;
    padding: 16px;
    margin-bottom: 12px;
    border-left: 4px solid var(--accent-color);
    transition: border-left-color 0.4s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.4s cubic-bezier(0.16, 1, 0.3, 1),
    opacity 0.4s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.03) inset;
    position: relative;
    overflow: hidden;
    color: rgba($text, 0.9);

    .notification::before {
      content: "";
      position: absolute;
      overflow: hidden;
      inset: 0;
      pointer-events: none;
      background-image: radial-gradient(circle, rgba(255, 255, 255, 0.08) 0%, transparent 70%);
      opacity: 0.3;
      mix-blend-mode: overlay;
      z-index: 0;
      border-radius: inherit;
    }

    &.success {
      --accent-color: var(--success-color);
      box-shadow: 0 12px 24px rgba($green, 0.4);
    }

    &.error {
      --accent-color: var(--error-color);
      box-shadow: 0 12px 24px rgba($red, 0.4);
    }

    &.info {
      --accent-color: var(--info-color);
      box-shadow: 0 12px 24px rgba($blue, 0.4);
    }

    &.blinking, &.blinked {
      --accent-color: var(--info-color);
      box-shadow: 0 12px 24px rgba($blue, 0.4);
    }

    &.enabled {
      --accent-color: var(--success-color);
      border-left-color: var(--success-color);
      box-shadow: 0 12px 24px rgba($green, 0.4);
    }

    &.disabled {
      --accent-color: var(--error-color);
      border-left-color: var(--error-color);
      box-shadow: 0 12px 24px rgba($red, 0.4);
    }
  }

  .icon-container {
    position: relative;
    flex-shrink: 0;
    width: 40px;
    height: 40px;
  }

  .icon {
    width: 100%;
    height: 100%;
    background-position: center;
    background-repeat: no-repeat;
    background-size: 24px;
    border-radius: 12px;
    transition: all 0.3s ease;

    &.success {
      background-image: url("/img/hud/notification/icon-success.svg");
      background-color: rgba($green, 0.12);
    }

    &.error {
      background-image: url("/img/hud/notification/icon-error.svg");
      background-color: rgba($red, 0.12);
    }

    &.info {
      background-image: url("/img/hud/notification/icon-info.svg");
      background-color: rgba($blue, 0.12);
    }

    &.blinking, &.blinked {
      background-image: none;

      .svg-container {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        transform-origin: center;
        transition: transform 0.2s cubic-bezier(0.68, -0.55, 0.27, 1.55);
      }

      svg {
        width: 40px;
        height: 40px;
        color: white;
      }
    }

    &.blinking .svg-container {
      animation: rotate 1s linear infinite;
    }

    &.blinked .svg-container {
      animation: none;
    }

    &.blinking svg {
      animation: rotate 1s linear infinite;
    }

    &.blinked svg {
      animation: none;
    }

    &.blinking svg {
      animation: rotate 1s linear infinite;
    }

    &.enabled, &.disabled {
      background-image: url("/img/hud/notification/icon-toggle.svg");
      background-color: rgba(var(--accent-color), 0.12);
    }

    &.enabled,
    &.disabled {
      background-image: none;

      svg {
        width: 40px;
        height: 40px;
        fill: var(--accent-color);
        transition: fill 0.3s ease;

        .toggle-track {
          fill: rgba(var(--accent-color), 0.12);
        }

        .toggle-slider {
          fill: var(--accent-color);
          transition: fill 0.1s ease,
          transform 0.3s cubic-bezier(0.68, -0.55, 0.27, 1.55);
          transform-box: fill-box;
          transform-origin: center;
          will-change: transform;
        }
      }
    }

    &.enabled {
      --accent-color: #{$green};

      svg .toggle-slider {
        transform: translateX(0);
      }
    }

    &.disabled {
      --accent-color: #{$red};

      svg .toggle-slider {
        transform: translateX(-10px);
      }
    }
  }

  .content {
    flex: 1;
    min-width: 0;
  }

  .title {
    font-size: 14px;
    font-weight: 600;
    color: $text;
    margin-bottom: 4px;
    line-height: 1.4;
  }

  .message {
    font-size: 13px;
    color: $subtext1;
    line-height: 1.5;
    white-space: pre-wrap;
  }

  .fade {
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  .fade.active {
    opacity: 1;
  }

  .progress-container {
    height: 4px;
    width: 100%;
    margin-top: 10px;
    background: rgba(20, 20, 20, 0.08);
    border-radius: 2px;
    overflow: hidden;
  }

  .progress-bar {
    height: 100%;
    border-radius: 2px;
    transition: width 0.1s linear;

    &.enabled {
      background: linear-gradient(90deg, color.mix($green, $text, 20%) 0%, $green 100%);
    }

    &.disabled {
      background: linear-gradient(90deg, color.mix($red, $text, 20%) 0%, $red 100%);
    }

    &.success {
      background: linear-gradient(90deg, color.mix($green, $text, 30%) 0%, $green 100%);
    }

    &.error {
      background: linear-gradient(90deg, color.mix($red, $text, 30%) 0%, $red 100%);
    }

    &.info {
      background: linear-gradient(90deg, color.mix($blue, $text, 30%) 0%, $blue 100%);
    }

    &.blinking, &.blinked {
      background: linear-gradient(90deg, color.mix($blue, $text, 30%) 0%, $blue 100%);
    }
  }

  @keyframes rotate {
    from {
      transform: rotate(0deg) scale(1);
    }
    to {
      transform: rotate(360deg) scale(1);
    }
  }
</style>
