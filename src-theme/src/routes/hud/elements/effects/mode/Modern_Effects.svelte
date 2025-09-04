<script lang="ts">
    import { listen } from "../../../../../integration/ws";
    import type { ClientPlayerDataEvent } from "../../../../../integration/events";
    import type { StatusEffect } from "../../../../../integration/types";
    import { fade} from 'svelte/transition';
    import { REST_BASE } from "../../../../../integration/host";
    import {expoOut} from "svelte/easing";
    import {flip} from "svelte/animate";
    import {springTransition} from "../../../../../util/animate_utils";

    let effects: StatusEffect[] = [];

    function getIdentifierName(effectId: string, amplifier: number): string {
        const id = effectId.replace(/^minecraft:/, '');
        const words = id.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1));
        return words.join(' ') + (amplifier + 1);
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        effects = [...event.playerData.effects].sort((a, b) => {
            const lengthDiff = b.localizedName.length - a.localizedName.length;
            if (lengthDiff !== 0) return lengthDiff;
            return b.amplifier - a.amplifier;
        });
    });

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
                class="effect {getWarnClass(e.duration)} hud-container"
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
                {getIdentifierName(e.effect, e.amplifier + 1)}
                </span>
                <div class="progress-bar">

                    <div class="progress" style="width: { (e.duration / (e.duration || 600)) * 100 }%;"></div>
                </div>
                <span
                        class="duration"
                        in:fade={{ delay: 200 }}
                >
                    {formatTime(e.duration)}
                </span>
            </div>
        </div>
    {/each}
</div>

<style lang="scss">
  @use "../../../../../colors" as *;

  .effects {
    position: relative;
    display: inline-flex;
    min-width: max-content;
    flex-direction: column-reverse;
    gap: 4px;
    white-space: nowrap;
    font-family: 'Alibaba', sans-serif;
  }

  .effect {
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

    &.warn-1 {
      background-color: rgba(255, 0, 0, 0.2);
      border: 1px solid #ff0000;
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
      background-color: rgba(255, 85, 85, 0.2);
      border: 1px solid #ff5555;

      .duration {
        background: linear-gradient(135deg, #ff5555, #ff9999);
        filter: drop-shadow(0 2px 10px rgba(255, 85, 85, 0.3));
        background-clip: text;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }

    &.warn-3 {
      background-color: rgba(255, 140, 0, 0.2);
      border: 1px solid #ff8c00;

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
