<script lang="ts">
    import { listen } from "../../../../../integration/ws";
    import type { ClientPlayerDataEvent } from "../../../../../integration/events";
    import type { StatusEffect } from "../../../../../integration/types";
    import {fade, fly} from 'svelte/transition';
    import {expoInOut} from "svelte/easing";
    import {REST_BASE} from "../../../../../integration/host";
    import {forcedEnglish} from "../Effects";

    let effects: StatusEffect[] = [];
    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        effects = [...event.playerData.effects].sort((a, b) => {
            const nameA = getIdentifierName(a.effect, a.amplifier);
            const nameB = getIdentifierName(b.effect, b.amplifier);

            const lengthDiff = nameB.length - nameA.length;
            if (lengthDiff !== 0) return lengthDiff;
            return b.amplifier - a.amplifier;
        });

    });

    function getIdentifierName(effectId: string, amplifier: number): string {
        const id = effectId.replace(/^minecraft:/, '');
        const words = id.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1));
        return words.join(' ') + (amplifier + 1);
    }

    function formatTime(duration: number): string {
        return new Date(((duration / 20) | 0) * 1000)
            .toISOString()
            .substring(14, 19);
    }

    function getWarnClass(ticks: number): string {
        const seconds = ticks / 20;
        if (seconds < 3) return "warn-1";
        if (seconds < 10) return "warn-2";
        if (seconds < 30) return "warn-3";
        return "";
    }
    function getEffectIcon(effect: string): string {
        const effectId = effect.replace(/^minecraft:/, '');
        return `${REST_BASE}/api/v1/client/resource/effectTexture?id=minecraft:${effectId}`;
    }

</script>
<div class="effects">
    {#each effects as e (`${e.effect}-${e.amplifier}`)}
        <div
                class="effect {getWarnClass(e.duration)}"
                transition:fly={{duration: 700, x: 50, easing: expoInOut}}
        >
            <img
                    class="effect-icon"
                    src={getEffectIcon(e.effect)}
                    alt={e.localizedName}
                    in:fade={{ delay: 100 }}
            />
            <span
                    class="name"
                    style="color: {'#' + e.color.toString(16).padStart(6, '0')};
                     filter: drop-shadow(0 2px 10px rgba({e.color >> 16 & 255},{e.color >> 8 & 255}, {e.color & 255}, 0.3));"
                    in:fade={{ delay: 100 }}
            >
                {#if $forcedEnglish}
                    {getIdentifierName(e.effect, e.amplifier)}
                {:else}
                    {e.localizedName} {e.amplifier + 1}
                {/if}
            </span>
            <span
                    class="duration"
                    in:fade={{ delay: 200 }}
            >
        {formatTime(e.duration)}
      </span>
        </div>
    {/each}
</div>
<style lang="scss">
  @use "../../../../../colors.scss" as *;

  .effects {
    position: relative;
    display: inline-flex;
    min-width: max-content;
    flex-direction: column-reverse;
    align-items: flex-end;
    gap: -8px;
    white-space: nowrap;
    font-family: 'Alibaba', sans-serif;
  }
  .effect-icon {
    height: 32px;
    width: 32px;
    filter: drop-shadow(0 0 4px rgba($base, 0.5));
  }
  .effect {
    font-weight: 600;
    font-size: 18px;
    text-align: left;
    border-radius: 8px;
    transition: background 0.3s ease, transform 0.2s ease;
    display: flex;
    align-items: center;
    gap: 4px;
    transform-origin: right center;

    &.warn-1 {
      color: #ff0000;
      animation: pulse 0.5s infinite alternate;

      .duration {
        background: linear-gradient(135deg, #ff0000, #ff6666);
        filter: drop-shadow(0 2px 10px rgba(255, 0, 0, 0.3));
        background-clip: text;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }

    &.warn-2 {
      color: #ff5555;

      .duration {
        background: linear-gradient(135deg, #ff5555, #ff9999);
        filter: drop-shadow(0 2px 10px rgba(255, 85, 85, 0.3));
        background-clip: text;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }

    &.warn-3 {
      color: #ff8c00;

      .duration {
        background: linear-gradient(135deg, #ff8c00, #ffbb66);
        filter: drop-shadow(0 2px 10px rgba(255, 140, 0, 0.3));
        background-clip: text;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }

    &:not(.warn-1):not(.warn-2):not(.warn-3) .duration {
      color: #CCCCCC;
      filter: drop-shadow(0 2px 10px rgba(255, 255, 255, 0.3));
    }
  }

  @keyframes pulse {
    from {
      opacity: 1;
    }
    to {
      opacity: 0.7;
    }
  }
</style>
