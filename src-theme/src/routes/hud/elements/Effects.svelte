<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {ClientPlayerDataEvent, ClientPlayerEffectEvent} from "../../../integration/events";
    import type {StatusEffect} from "../../../integration/types";
    import {effectTextureUrl} from "../../../integration/rest";
    import {intToHex} from "../../../integration/util";

    /** Effects with fewer ticks left blink. */
    const EXPIRING_TICKS = 200;

    interface ShownEffect extends StatusEffect {
        /** The longest duration seen at this level, which is a full bar: the client only knows the time left. */
        fullDuration: number;
    }

    let effects: ShownEffect[] = [];

    function updateEffects(next: StatusEffect[]) {
        effects = next.map(e => {
            const shown = effects.find(s => s.effect === e.effect && s.amplifier === e.amplifier);
            return {...e, fullDuration: Math.max(e.duration, shown?.fullDuration ?? 0)};
        });
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        updateEffects(event.playerData.effects);
    });

    listen("clientPlayerEffect", (event: ClientPlayerEffectEvent) => {
        updateEffects(event.effects);
    });

    function formatTime(duration: number): string {
        if (duration === -1) {
            return "*:*";
        }

        const totalSeconds = Math.floor(duration / 20);
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;

        return `${minutes}:${seconds.toString().padStart(2, "0")}`;
    }

    function formatAmplifier(n: number): string {
        return (n + 1).toString();
    }

    function progress(e: ShownEffect): number {
        return e.fullDuration > 0 ? e.duration / e.fullDuration * 100 : 0;
    }

    // Beacons and conduits renew their effects every few seconds, so those never run out while in range
    function isExpiring(e: StatusEffect): boolean {
        return !e.infinite && !e.ambient && e.duration <= EXPIRING_TICKS;
    }
</script>

{#if effects.length > 0}
    <div class="effects">
        {#each effects as e}
            <div class="effect" class:expiring={isExpiring(e)}>
                <div class="row">
                    <img class="effect-icon" src={effectTextureUrl(e.effect)} alt={e.localizedName}/>
                    <span class="name">{e.localizedName}  <span
                            class="amplifier">{formatAmplifier(e.amplifier)}</span></span>
                    <span class="duration">{formatTime(e.duration)}</span>
                </div>
                {#if !e.infinite}
                    <div class="progress">
                        <div class="thumb"
                             style="width: {progress(e)}%; background-color: {intToHex(0xff000000 | e.color)};"></div>
                    </div>
                {/if}
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">

  .effects {
    display: flex;
    flex-direction: column;
    gap: 4px;
    background-color: var(--effects-background-color);
    border-radius: 5px;
    padding: 4px 6px;
  }

  .effect {
    display: flex;
    flex-direction: column;
    gap: 2px;

    &.expiring {
      animation: blink 1s steps(2, jump-none) infinite;

      .duration {
        color: var(--effects-expiring-color);
      }
    }
  }

  .row {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
    font-size: 14px;

    .effect-icon {
      width: 16px;
      height: 16px;
      image-rendering: pixelated;
      image-rendering: -moz-crisp-edges;
      image-rendering: crisp-edges;
    }

    .name {
      color: var(--effects-name-color);
    }

    .amplifier {
      color: var(--effects-amplifier-color);
    }

    .duration {
      margin-left: auto;
      font-family: monospace;
      color: var(--effects-duration-color);
      font-size: 12px;
    }
  }

  .progress {
    height: 2px;
    border-radius: 1px;
    overflow: hidden;
    background-color: var(--effects-progress-background-color);

    .thumb {
      height: 100%;
    }
  }

  @keyframes blink {
    from {
      opacity: 1;
    }

    to {
      opacity: 0.35;
    }
  }
</style>
