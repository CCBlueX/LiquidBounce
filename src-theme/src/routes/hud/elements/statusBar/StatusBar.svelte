<script lang="ts">
    import {onMount} from "svelte";
    import {listen} from "../../../../integration/ws";
    import {getPlayerData} from "../../../../integration/rest";
    import type {ClientPlayerDataEvent} from "../../../../integration/events";
    import type {PlayerData} from "../../../../integration/types";
    import {fade} from "svelte/transition";
    import {calcArmorValue} from "../../../../util/Client/calcArmorValue";

    let playerData: PlayerData | null = null;
    type BarKey = 'armor' | 'food';

    function updatePlayerData(newData: PlayerData) {
        const updatedData = {...newData};
        if (updatedData.armorItems) {
            updatedData.armor = calcArmorValue(updatedData.armorItems);
        }
        playerData = updatedData;
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        updatePlayerData(event.playerData);
    });

    onMount(async () => {
        updatePlayerData(await getPlayerData());
    });


    $: statusBars = [
        {
            key: "food" as BarKey,
            condition: () => playerData?.food !== undefined || (playerData?.air !== undefined && playerData?.maxAir !== undefined),
            max: () => playerData?.air !== undefined && playerData?.air < (playerData?.maxAir ?? Infinity) ? (playerData?.maxAir ?? 0) : 20,
            value: () => playerData?.air !== undefined && playerData?.air < (playerData?.maxAir ?? Infinity) ? playerData?.air : playerData?.food,
            color: "white",
            size: 'large' as const,
            topOffset: "calc(50% - 25px)",
            transform: "skew(30deg, 0deg)"
        },
        {
            key: "armor" as BarKey,
            condition: () => playerData?.armor !== undefined,
            max: 20,
            value: () => playerData?.armor,
            color: "#00ffff",
            size: 'large' as const,
            topOffset: "calc(50% + 5px)",
            transform: "skew(-30deg, 0deg)"
        },
    ];
</script>

{#if playerData && playerData.gameMode !== "spectator"}
    <div class="hotbar" transition:fade={{ duration: 300 }}>
        {#each statusBars as bar (bar.key)}
            {#if bar.condition()}
                {@const maxValue = typeof bar.max === 'function' ? bar.max() : bar.max}
                {@const currentValue = bar.value() ?? 0}
                {@const percent = Math.max(0, Math.min(currentValue, maxValue)) / maxValue * 100}
                <div
                        class="xguiBar {bar.size}"
                        style="
                        position: absolute;
                        left: calc(50% - 100px);
                        top: {bar.topOffset};
                        color: {bar.key === 'armor' ? bar.color :
                               percent >= 50 ? 'white' :
                               percent >= 25 ? 'orange' : 'red'};
                        box-shadow: 0px 0px 5px {bar.key === 'armor' ? '#00ffff' : percent >= 50 ? 'white' : percent >= 25 ? 'orange' : 'red'},
                                    0px 0px 5px inset,
                                    {percent}em 0px 0px 0px inset {bar.key === 'armor' ? 'rgba(0, 255, 255, 0.5)' : percent >= 50 ? 'rgba(255, 255, 255, 0.5)' : percent >= 25 ? 'rgba(255, 165, 0, 0.5)' : 'rgba(255, 0, 0, 0.5)'};
                        background-color: {bar.key === 'armor' ? 'rgba(0, 128, 128, 0.3)' : 'transparent'};
                        transform: {bar.transform};
                    "
                >
                </div>
            {/if}
        {/each}
    </div>
{/if}

<style lang="scss">
  @use "../../../../colors" as *;

  .hotbar {
    position: relative;
    width: 200px;
    height: 50px;
    margin: 0 auto;
  }

  .xguiBar {
    position: relative;
    box-sizing: border-box;
    border: solid 1px;
    width: 200px;
    height: 20px;
    font-size: 2px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    padding-left: 12px;
    transition: color 0.3s ease, box-shadow 0.3s ease;

    &.large {
      width: 200px;
      height: 20px;
      font-size: 2px;
      border-radius: 10px;
    }

    &.small {
      width: 50px;
      height: 5px;
      font-size: 0.5px;
      border-radius: 3px;
    }
  }
</style>
