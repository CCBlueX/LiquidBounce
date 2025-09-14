<script lang="ts">
    import {deathCount, kills, wins} from "../../../../../util/Theme/SessionManager";
    import {listen} from "../../../../../integration/ws.js";
    import type {ClientPlayerDataEvent} from "../../../../../integration/events";
    import type {PlayerData} from "../../../../../integration/types";
    import {expoInOut} from "svelte/easing";
    import {fly} from 'svelte/transition';

    let playerData: PlayerData | null = null;
    let currentMinutes = 0;
    let progressPercentage = 0;
    let strokeDashOffset = 0;
    $: if (playerData) {
        currentMinutes = Math.floor(playerData.playTime / 60);
        const totalSeconds = playerData.playTime;

        const secondsInCurrentMinute = totalSeconds % 60;

        strokeDashOffset = Math.PI * 2 * 42 * (1 - secondsInCurrentMinute / 60);


        progressPercentage = (currentMinutes / 60) * 100;
    }
    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        playerData = event.playerData;
        kills.set(playerData.killsCount ?? 0);
        deathCount.set(playerData.deathCount ?? 0);
        wins.set(playerData.winsCount ?? 0);
    });

</script>

<div class="session-info hud-container" style={`--progress: ${progressPercentage}`}
     transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
    <div class="time-circle">
        <div class="time-label">
            {currentMinutes}<br/>
            <span style="font-size: 16px;">min</span>
        </div>
        <svg class="progress-ring" height="96" viewBox="0 0 96 96" width="96">
            <circle
                    class="progress-ring-circle"
                    cx="48"
                    cy="48"
                    fill="transparent"
                    r="42"
                    stroke="url(#progressGradient)"
                    stroke-dasharray="{Math.PI * 2 * 42}"
                    stroke-dashoffset={strokeDashOffset}
                    stroke-linecap="round"
                    stroke-width="10"
            />
            <defs>
                <linearGradient id="progressGradient" x1="0%" x2="100%" y1="0%" y2="100%">
                    <stop offset="0%" stop-color="var(--secondary-color)"/>
                    <stop offset="100%" stop-color="var(--primary-color)"/>
                </linearGradient>
            </defs>
        </svg>
    </div>
    <div class="stats">
        <div class="stat-line">
            <div class="icon-container">
                <div class="icon-bg"></div>
                <img alt="Wins" src="img/hud/sessioninfo/won.svg"/>
            </div>
            <span>Wins:</span><span class="value">{$wins}</span>
        </div>
        <div class="stat-line">
            <div class="icon-container">
                <div class="icon-bg"></div>
                <img alt="Kills" src="img/hud/sessioninfo/kills.svg"/>
            </div>
            <span>Kills:</span><span class="value">{$kills}</span>
        </div>
        <div class="stat-line">
            <div class="icon-container">
                <div class="icon-bg"></div>
                <img alt="Deaths" src="img/hud/sessioninfo/death.svg"/>
            </div>
            <span>Deaths:</span><span class="value">{$deathCount}</span>
        </div>
    </div>
</div>


<style lang="scss">
  @use "../../../../../colors" as *;

  .session-info {
    --ring-thickness: 10px;
    font-family: 'Alibaba', sans-serif;
    display: flex;
    align-items: center;
    padding: 10px 16px;
    width: fit-content;
    color: white;

  }

  .time-circle {
    position: relative;
    width: 96px;
    height: 96px;
    margin-right: 16px;
    filter: drop-shadow(0 2px 4px color-mix(in srgb, var(--primary-color) 30%, transparent));

    .progress-ring {
      transform: rotate(-90deg);

      &-circle {
        transition: stroke-dashoffset 0.3s ease;
      }
    }
  }

  .time-label {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-align: center;
    line-height: 0.8;
    font-size: 24px;
    z-index: 1;
  }

  .stats {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 6px;
  }

  .stat-line {
    display: grid;
    grid-template-columns: 28px auto min-content;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    min-width: 120px;
  }

  .icon-container {
    position: relative;
    width: 24px;
    height: 24px;
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
  }

  .icon-bg {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 4px;
    background: rgba($base, 0.05);
  }

  .stat-line span:first-of-type {
    white-space: nowrap;
  }

  .stat-line img {
    width: 20px;
    height: 20px;
    opacity: 0.8;
    z-index: 1;
  }

  .stat-line .value {
    text-align: right;
    min-width: 40px;
    font-weight: bold;
    background-clip: text;
    background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    text-shadow: 0 0 1px color-mix(in srgb, var(--primary-color) 30%, transparent);
  }
</style>
