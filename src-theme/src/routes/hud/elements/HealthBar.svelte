<script lang="ts">
    import {listen} from "../../../integration/ws";
    import {getPlayerData} from "../../../integration/rest";
    import type {PlayerData} from "../../../integration/types";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import {onDestroy, onMount, tick} from "svelte";
    import {cubicOut} from 'svelte/easing';
    import {fade} from "svelte/transition";
    import {hsvToRgba} from "../../../util/color_utils";
    import {Tween} from "svelte/motion";

    let showHealthbar = false;
    let blink = false;
    let playerData: PlayerData | null = null;
    let iv: ReturnType<typeof setInterval> | null = null;

    const healthTweened = new Tween(0, {duration: 300, easing: cubicOut});
    const absorptionTweened = new Tween(0, {duration: 300, easing: cubicOut});
    const maxHealthTweened = new Tween(1, {duration: 300, easing: cubicOut});
    const totalTweened = new Tween(0, {duration: 800, easing: cubicOut});
    const prevHealthTweened = new Tween(0, {duration: 800, easing: cubicOut});
    const prevAbsorptionTweened = new Tween(0, {duration: 1000, easing: cubicOut});

    async function showDelayed() {
        await tick();
        await new Promise(res => setTimeout(res, 500));
        showHealthbar = true;
    }

    function fmt(n: number): string {
        const rounded = Math.round(n);
        return Math.abs(n - rounded) < 0.05
            ? `${rounded}`
            : n.toFixed(1);
    }

    function updatePlayerData(s: PlayerData) {
        playerData = s;
        healthTweened.set(s.health);
        absorptionTweened.set(s.absorption);
        maxHealthTweened.set(s.maxHealth);
        totalTweened.set(s.health + s.absorption);
        prevHealthTweened.set(s.health);
        prevAbsorptionTweened.set(s.absorption);
    }

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        updatePlayerData(e.playerData);
    });

    $: health = playerData ? playerData.health : 0;
    $: max = playerData ? playerData.maxHealth : 1;
    $: total = Math.max(healthTweened.current + absorptionTweened.current, maxHealthTweened.current, 1);
    $: healthPct = Math.min(Math.max(healthTweened.current / total, 0), 1) * 100;
    $: absorbPct = Math.min(Math.max(absorptionTweened.current / total, 0), 1) * 100;
    $: prevHealthPct = Math.min(Math.max(prevHealthTweened.current / total, 0), 1) * 100;
    $: prevAbsorbPct = Math.min(Math.max(prevAbsorptionTweened.current / total, 0), 1) * 100;
    $: prevPct = prevHealthPct + prevAbsorbPct;
    $: isLowHealth = health / max <= 0.25;

    $: {
        if (isLowHealth && !iv) {
            iv = setInterval(() => (blink = !blink), 500);
        } else if (!isLowHealth && iv) {
            clearInterval(iv);
            iv = null;
            blink = false;
        }
    }

    $: bgFlash = isLowHealth && blink ? "rgba(251,114,90,0.6)" : "transparent";
    $: hpColor = isLowHealth
        ? hsvToRgba(4, 60, 100, 0.7)
        : hsvToRgba(82, 68, 84, 0.7);
    $: abColor = "rgba(212,175,55,0.7)";
    $: barBgStyle = `linear-gradient(
        to bottom,
        ${bgFlash},
        rgba(154,216,31,0.1),
        rgba(20,20,20,0.5)
    )`;

    $: barStyle = `linear-gradient(to right,
    ${hpColor}   0%,
    ${hpColor}   ${healthPct}%,
    ${abColor}   ${healthPct + absorbPct}%,
    rgba(0,0,0,0.4) ${healthPct + absorbPct}%,
    rgba(0,0,0,0.4) 100%
    )`;

    $: fadeStyle = (() => {
        const curEnd = healthPct + absorbPct;
        if (prevPct <= curEnd) return "none";
        const hpStop = Math.max(prevHealthPct, curEnd);
        const abStop = prevPct;
        return `linear-gradient(to right,
        rgba(0,0,0,0) ${curEnd}%,
        ${hpColor} ${hpStop}%,
        ${abColor} ${abStop}%,
        rgba(0,0,0,0) ${abStop}%,
        rgba(0,0,0,0) 100%
    )`;
    })();

    onMount(async () => {
        updatePlayerData(await getPlayerData());
        await showDelayed();
    });
    onDestroy(() => iv && clearInterval(iv));

</script>
{#if showHealthbar && playerData && playerData.gameMode !== "spectator"}
    <div class="healthbar" transition:fade>
        {#if playerData.gameMode !== "creative"}
            <div class="status-container">
                <div class="status-wrapper">
                    <div class="level-stat">Lv. {playerData.experienceLevel}</div>
                    <div class="bar" style="--bar-bg: {barBgStyle}; background: {barStyle};">
                        {#if fadeStyle !== "none"}
                            <div class="fade" style="background: {fadeStyle};"></div>
                        {/if}
                    </div>
                    <div class="health-display">
                        <div class="left-group">
                            <span class="number current">{fmt(healthTweened.current)}</span>
                            {#if absorptionTweened.current > 0}
                                <span class="absorption">+{fmt(absorptionTweened.current)}</span>
                            {/if}
                        </div>
                        <span class="separator">/</span>
                        <span class="number max">{fmt(maxHealthTweened.current)}</span>
                    </div>
                </div>
            </div>
        {/if}
    </div>
{/if}

<style lang="scss">
  .healthbar {
    display: flex;
    justify-content: center;
    margin-bottom: 6px;
    align-items: center;
    font-family: "Genshin", sans-serif;
  }

  .status-container {
    width: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .status-wrapper {
    position: relative;
    width: 420px;
    height: 14px;
    display: flex;
    align-items: center;
  }

  .bar {
    width: 100%;
    height: 100%;
    background: var(--bar-bg);
    clip-path: polygon(
                    calc(2% + 0.5px) 0,
                    calc(98% - 0.5px) 0,
                    100% 50%,
                    calc(98% - 0.5px) 100%,
                    calc(2% + 0.5px) 100%,
                    0% 50%
    );
    border-radius: 7px;
    background-size: 100% 100%;
    background-repeat: no-repeat;
    overflow: hidden;
    position: relative;
    z-index: 1;
  }

  .fade {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background-repeat: no-repeat;
    background-size: 100% 100%;
    opacity: 0.5;
  }

  .health-display {
    position: absolute;
    inset: 0;
    font-size: 16px;
    font-weight: bold;
    color: #fff;
    text-shadow: 0 0 2px rgba(0, 0, 0, 0.9),
    0 0 4px rgba(0, 0, 0, 0.7),
    1px 1px 2px rgba(0, 0, 0, 0.6),
    -1px -1px 2px rgba(0, 0, 0, 0.6);


    padding: 0 2em;
  }


  .separator {
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);

    z-index: 2;
  }


  .left-group {
    position: absolute;
    right: calc(50% + 0.3em);
    top: 50%;
    transform: translateY(-50%);
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 0.2em;
    z-index: 1;
  }

  .number.max {
    position: absolute;
    left: calc(50% + 0.3em);
    top: 50%;
    transform: translateY(-50%);
    z-index: 1;
  }

  .number,
  .absorption {
    display: inline-block;
    font-feature-settings: "tnum";
    text-align: center;
  }

  .level-stat {
    position: absolute;
    right: calc(100% + 12px);
    font-size: 14px;
    top: 1.5px;
    color: rgba(255, 255, 255, 0.85);
    text-shadow: 0 0 2px rgba(0, 0, 0, 0.9),
    0 0 4px rgba(0, 0, 0, 0.7),
    1px 1px 2px rgba(0, 0, 0, 0.6),
    -1px -1px 2px rgba(0, 0, 0, 0.6);

    white-space: nowrap;
    line-height: 1;
  }
</style>
