<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import type {KeyBindingCPSEvent, KeyEvent, MouseButtonEvent} from "../../../../integration/events";
    import type {MinecraftKeybind} from "../../../../integration/types";

    export let flexBasis: string = '50px';
    export let key: MinecraftKeybind | undefined;
    export let asBar: boolean = false;
    export let showName: boolean = false;
    export let showCPS: boolean = false;

    let cps = 0;
    let active = false;
    let actived = false;

    listen("key", (e: KeyEvent) => {
        if (e.key !== key?.key.translationKey) return;
        if (e.action === 1 || e.action === 2) {
            active = true;
            actived = false;
        } else {
            active = false;
            actived = true;
            setTimeout(() => (actived = false), 200);
        }
    });
    listen("mouseButton", (e: MouseButtonEvent) => {
        if (e.key !== key?.key.translationKey) {
            return;
        }
        active = e.action === 1 || e.action === 2;
    });
    if (showCPS) {
        listen("keyBindingCPS", (e: KeyBindingCPSEvent) => {
            if (e.key !== key?.key.translationKey) {
                return;
            }
            cps = e.cps;
        });
    }
    $: displayName = (() => {
        if (!key) return "???";
        switch (key.bindName) {
            case "key.sprint":
                return "L Ctrl";
            case "key.sneak":
                return "L Shift";
            default:
                return key.key.localized;
        }
    })();

</script>
<div class="key" class:active class:actived class:asBar style="flex-basis: {flexBasis};">
    {#if showName && key?.bindName !== "key.jump"}
        {displayName}
    {:else if asBar || key?.bindName === "key.jump"}
        <div class="bar"></div>
    {/if}
    {#if showCPS}
        <span>{cps}</span>
    {/if}
</div>


<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as *;

  @keyframes activeEffect {
    0% {
      border-radius: 50%;
      transform: scale(0.1);
      opacity: 0.1;
    }
    10% {
      border-radius: 50%;
      transform: scale(0.3);
      opacity: 0.3;
    }
    100% {
      border-radius: inherit;
      transform: scale(1);
      opacity: 0.4;
    }
  }

  @keyframes activedEffect {
    0% {
      border-radius: inherit;
      transform: scale(1);
      opacity: 0.4;
    }
    100% {
      border-radius: 50%;
      transform: scale(0.1);
      opacity: 0;
    }
  }

  .key {
    height: 50px;
    color: $text;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: 900;
    text-transform: uppercase;
    position: relative;
    background: transparent;
    cursor: pointer;
    overflow: hidden;
    border-radius: 12px;
    will-change: transform, opacity;
    transition: box-shadow 0.1s ease;
    background: linear-gradient(
                    135deg,
                    rgba(20, 20, 20, 0.6) 0%,
                    rgba(color.adjust($base, $lightness: -5%), 0.5) 100%
    );
    border: 1px solid rgba(255, 255, 255, 0.08);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.03) inset;

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background: radial-gradient(
                      circle at 50% 0%,
                      color-mix(in srgb, var(--primary-color) 15%, transparent) 0%,
                      transparent 70%
      );
      pointer-events: none;
      z-index: -1;
      opacity: 0;
      transition: opacity 0.5s ease,
      transform 0.3s cubic-bezier(0.2, 0.8, 0.4, 1.2);
    }

    .bar {
      width: 33%;
      height: 4px;
      background-color: currentColor;
      border-radius: 2px;
    }

    &:active {
      box-shadow: 0 0 10px $key-color;

    }

    &:hover {
      box-shadow: 0 0 6px rgba($key-color, 0.3);
    }

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background: rgba($base, 0.2);
      border-radius: inherit;
      z-index: 0;
    }

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      background: rgba($key-color, 0.8);
      border-radius: 50%;
      border: 1px solid rgba(255, 255, 255, 0.8);
      opacity: 0;
      z-index: 1;
      transform-origin: center;
      pointer-events: none;
      transform: scale(0.1);
    }

    &.active::after {
      animation: activeEffect 0.2s ease forwards;
    }

    &.actived::after {
      animation: activedEffect 0.2s ease forwards;
    }
  }
</style>
