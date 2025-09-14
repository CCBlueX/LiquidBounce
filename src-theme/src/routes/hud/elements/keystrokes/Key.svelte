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
    let ripples: { id: number }[] = [];
    let rippleCounter = 0;
    let isPressed = false;

    listen("key", (e: KeyEvent) => {
        if (e.key !== key?.key.translationKey) return;
        if (e.action === 1 && !isPressed) {
            isPressed = true;
            ripples = [...ripples, { id: rippleCounter++ }];
            setTimeout(() => {
                ripples = ripples.filter(r => r.id !== ripples[0].id);
            }, 400);
        } else if (e.action === 0) {
            isPressed = false;
        }
    });

    listen("mouseButton", (e: MouseButtonEvent) => {
        if (e.key !== key?.key.translationKey) return;
        if (e.action === 1 && !isPressed) {
            isPressed = true;
            ripples = [...ripples, { id: rippleCounter++ }];
            setTimeout(() => {
                ripples = ripples.filter(r => r.id !== ripples[0].id);
            }, 400);
        } else if (e.action === 0) {
            isPressed = false;
        }
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

<div class="key" style="flex-basis: {flexBasis};">
    {#if showName && key?.bindName !== "key.jump"}
        {displayName}
    {:else if asBar || key?.bindName === "key.jump"}
        <div class="bar"></div>
    {/if}
    {#if showCPS}
        <span>{cps}</span>
    {/if}
    {#each ripples as ripple (ripple.id)}
        <div class="ripple"></div>
    {/each}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as *;

  @keyframes rippleEffect {
    0% {
      transform: scale(0);
      opacity: 0.4;
    }
    100% {
      transform: scale(2.5);
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
      transition: opacity 0.5s ease;
    }

    .bar {
      width: 33%;
      height: 4px;
      background-color: currentColor;
      border-radius: 2px;
    }

    .ripple {
      position: absolute;
      inset: 0;
      background: rgba($key-color, 0.3);
      border-radius: 12px;
      z-index: 1;
      transform: scale(0);
      transform-origin: center;
      pointer-events: none;
      animation: rippleEffect 0.4s ease-out forwards;
    }
  }
</style>
