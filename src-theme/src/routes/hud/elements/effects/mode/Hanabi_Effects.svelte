<script lang="ts">
    import { listen } from "../../../../../integration/ws";
    import type { ClientPlayerDataEvent } from "../../../../../integration/events";
    import type { StatusEffect } from "../../../../../integration/types";
    import { fade} from 'svelte/transition';
    import { forcedEnglish } from "../Effects"
    import { REST_BASE } from "../../../../../integration/host";
    import {expoOut} from "svelte/easing";
    import {flip} from "svelte/animate";
    import {springTransition} from "../../../../../util/animate_utils";
    
    type EffectWithMax = StatusEffect & { maxDuration: number };
    let effects: EffectWithMax[] = [];

    function getIdentifierName(effectId: string, amplifier: number): string {
        const id = effectId.replace(/^minecraft:/, '');
        const words = id.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1));
        return words.join(' ') + (amplifier + 1);
    }

    const initialDurations = new Map<string, number>();



    function formatTime(duration: number): string {
        return new Date(((duration / 20) | 0) * 1000)
            .toISOString()
            .substring(14, 19);
    }

    function getEffectIcon(effect: string): string {
        const effectId = effect.replace(/^minecraft:/, '');
        return `${REST_BASE}/api/v1/client/resource/effectTexture?id=minecraft:${effectId}`;
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        effects = event.playerData.effects.map(e => {
            const key = `${e.effect}-${e.amplifier}`;

            if (!(initialDurations.has(key)) || e.duration > (initialDurations.get(key) ?? 0)) {
                initialDurations.set(key, e.duration);
            }

            return {
                ...e,
                maxDuration: initialDurations.get(key) ?? e.duration
            };
        }).sort((a, b) => {
            const nameA = $forcedEnglish
                ? getIdentifierName(a.effect, a.amplifier)
                : `${a.localizedName} ${a.amplifier + 1}`;
            const nameB = $forcedEnglish
                ? getIdentifierName(b.effect, b.amplifier)
                : `${b.localizedName} ${b.amplifier + 1}`;

            const lengthDiff = nameB.length - nameA.length;
            if (lengthDiff !== 0) return lengthDiff;
            return b.amplifier - a.amplifier;
        });
    });

</script>

<div class="effects">
    {#each effects as e (`${e.effect}-${e.amplifier}`)}
        <div
                class="effect hud-container"
                style="--progress: {e.duration / e.maxDuration}"
                in:springTransition={{ delay: 100 }}
                animate:flip={{duration: 350, easing: expoOut}}
        >
            <img
                    class="effect-icon"
                    src={getEffectIcon(e.effect)}
                    alt={e.localizedName}
                    in:fade={{ delay: 100 }}
            />
            <div class="effect-info">
                <span
                        class="name"
                        style="color: {'#' + e.color.toString(16).padStart(6, '0')};
                     filter: drop-shadow(0 2px 10px rgba({e.color >> 16 & 255},
                      {e.color >> 8 & 255}, {e.color & 255}, 0.3));"
                        in:fade={{ delay: 100 }}
                >
                    {#if $forcedEnglish}
                        {getIdentifierName(e.effect, e.amplifier)}
                    {:else}
                        {e.localizedName} {e.amplifier + 1}
                    {/if}
                </span>
                <span class="duration" in:fade={{ delay: 200 }}>
                    {formatTime(e.duration)}
                </span>
            </div>
        </div>
    {/each}
</div>


<style lang="scss">
  @use "../../../../../colors" as *;
  .effect {
    position: relative;
    font-weight: 600;
    font-size: 18px;
    text-align: left;
    border-radius: 8px;
    border: 1px solid transparent;
    transition: background 0.3s ease, transform 0.2s ease;
    display: flex;
    align-items: center;
    gap: 8px;
    transform-origin: left center;
    overflow: hidden;

    &::after {
      content: "";
      position: absolute;
      left: 0;
      bottom: 0;
      top: 0;
      width: calc(100% * var(--progress));
      background: linear-gradient(to right, rgba(100, 150, 255, 0.3), rgba(100, 150, 255, 0));
      z-index: 0;
      transition: width 0.2s linear;
    }

    > * {
      position: relative;
      z-index: 1;
    }

    .effect-icon {
      height: 32px;
      width: 32px;
      filter: drop-shadow(0 0 4px rgba($base, 0.5));
    }

    .effect-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .name {
      font-weight: 600;
      font-size: 16px;
      color: #fff;
    }

    .duration {
      font-size: 14px;
      color: #ccc;
    }
  }

</style>
