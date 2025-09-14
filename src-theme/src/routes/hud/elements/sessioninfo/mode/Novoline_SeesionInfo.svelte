<script lang="ts">
    import { listen } from "../../../../../integration/ws.js";
    import type { ClientPlayerDataEvent } from "../../../../../integration/events";
    import type { PlayerData } from "../../../../../integration/types";
    import { fly } from 'svelte/transition';
    import { expoInOut } from "svelte/easing";
    import Line from "../../../common/Trims/Line.svelte";
    import {kills, wins} from "../../../../../util/Theme/SessionManager";
    export let settings: { [name: string]: any };

    let playerData: PlayerData | null = null;

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        playerData = event.playerData;
        kills.set(playerData.killsCount ?? 0);
        wins.set(playerData.winsCount?? 0);
    });
</script>

<div class="session-info hud-container" transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
    <div class="title">
        <span>Session Information</span>
    </div>
    {#if settings?.divider}
        <Line gradient={settings?.gradient}/>
    {/if}
    <div class="stats">
        <div class="stat-line">
            <span>Play Time:</span>
            <span class="value">{playerData?.playTime != null
                ? `${String(Math.floor(playerData.playTime / 3600)).padStart(2, "0")}:` +
                `${String(Math.floor((playerData.playTime % 3600) / 60)).padStart(2, "0")}:` +
                `${String(playerData.playTime % 60).padStart(2, "0")}`
                : "00:00:00"}
            </span>
        </div>
        <div class="stat-line">
            <span>Games Won:</span><span class="value">{$wins}</span>
        </div>
        <div class="stat-line">
            <span>Players Killed:</span><span class="value">{$kills}</span>
        </div>
        <div class="stat-line">
            <span>Staff/Watchdog Bans:</span><span class="value">0/0</span>
        </div>
    </div>
</div>

<style lang="scss">
  @use "../../../../../colors" as *;

  .session-info {
    display: flex;
    flex-direction: column;
    min-width: 300px;
    padding: 10px 16px;
    width: fit-content;
    color: white;
  }
  .session-info :global(.line) {
    margin: 0.25em 0;
  }

  .title {
    display: flex;
    justify-content: center;
    font-size: 1em;
    font-weight: bold;
    letter-spacing: 0.1em;
    margin-bottom: 0.5em;
  }

  .stats {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .stat-line {
    display: grid;
    grid-template-columns: auto min-content;
    align-items: center;
    gap: 8px;
    font-size: 16px;
  }

  .stat-line .value {
    text-align: right;
    min-width: 40px;
    font-weight: bold;
    color: white;
  }
</style>
