<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import type {StatusEffect} from "../../../integration/types";
    import {effectTextureUrl} from "../../../integration/rest";

    let effects: StatusEffect[] = [];

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        effects = event.playerData.effects;
    });

    function formatTime(duration: number): string {
        if (duration === -1) {
            return "*:*";
        }
        const totalSeconds = Math.floor(duration / 20);
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    }

    function convertToRoman(n: number): string {
        return (n + 1).toString();
    }

    function getEffectIcon(effectId: string): string {
        return effectTextureUrl(effectId);
    }
</script>

<div class="effects-container">
    <div class="effects-title">Potions</div>
    <div class="effects">
        {#each effects as e}
            <div class="effect">
                <img class="effect-icon" src={getEffectIcon(e.effect)} alt={e.localizedName} />
                <span class="name">{e.localizedName} <span class="amplifier">{convertToRoman(e.amplifier)}</span></span>
                <span class="duration">{formatTime(e.duration)}</span>
            </div>
        {/each}
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .effects-container {
    display: flex;
    flex-direction: column;
    background-color: #2b2b36;
    border-radius: 4px;
    padding: 4px;
    gap: 2px;
  }

  .effects-title {
    color: white;
    font-size: 12px;
    font-weight: 500;
    text-align: center;
    padding-bottom: 2px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    margin-bottom: 2px;
  }

  .effects {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .effect {
    display: flex;
    align-items: center;
    gap: 4px;
    font-weight: 500;
    font-size: 12px;
    padding: 2px 4px;
    border-radius: 2px;

    .effect-icon {
      width: 16px;
      height: 16px;
      border-radius: 1px;
      flex-shrink: 0;
      image-rendering: pixelated;
      image-rendering: -moz-crisp-edges;
      image-rendering: crisp-edges;
    }

    .name {
      flex: 1;
      color: white;
    }

    .amplifier {
      color: #ff5555;
    }

    .duration {
      font-family: monospace;
      color: white;
      font-size: 11px;
      min-width: 30px;
      text-align: right;
    }
  }
</style>