<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {ClientPlayerDataEvent, ClientPlayerEffectEvent} from "../../../integration/events";
    import type {StatusEffect} from "../../../integration/types";
    import {effectTextureUrl} from "../../../integration/rest";

    let effects: StatusEffect[] = [];

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        effects = event.playerData.effects;
    });

    listen("clientPlayerEffect", (event: ClientPlayerEffectEvent) => {
        effects = event.effects;
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
</script>

{#if effects.length > 0}
    <div class="effects">
        {#each effects as e}
            <div class="effect">
                <img class="effect-icon" src={effectTextureUrl(e.effect)} alt={e.localizedName}/>
                <span class="name">{e.localizedName}  <span
                        class="amplifier">{formatAmplifier(e.amplifier)}</span></span>
                <span class="duration">{formatTime(e.duration)}</span>
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">
  @use "../../../colors.scss" as *;

  .effects {
    display: flex;
    flex-direction: column;
    gap: 4px;
    background-color: $effects-background-color;
    border-radius: 5px;
    padding: 4px 6px;
  }

  .effect {
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
      color: $effects-name-color;
    }

    .amplifier {
      color: $effects-amplifier-color;
    }

    .duration {
      margin-left: auto;
      font-family: monospace;
      color: $effects-duration-color;
      font-size: 12px;
    }
  }
</style>
