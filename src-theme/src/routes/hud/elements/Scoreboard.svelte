<script lang="ts">
    import {listen} from "../../../integration/ws";
    import {fly} from "svelte/transition";
    import {expoInOut} from "svelte/easing";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import type {PlayerData, Scoreboard} from "../../../integration/types";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import {scoreboardIP} from "../../../util/Theme/ThemeManager";
    import GradientAnimatedText from "../common/FontRenderer/GradientAnimatedText.svelte";

    export let settings: { [name: string]: any };

    let scoreboard: Scoreboard | null = null;
    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        const playerData: PlayerData = e.playerData;
        scoreboard = playerData.scoreboard;
    });
</script>


{#if scoreboard}
    <div class="scoreboard hud-container" transition:fly|global={{duration: 500, x: 50, easing: expoInOut}}>
        {#if scoreboard.header}
            <div class="header">
                <TextComponent fontSize={18} allowPreformatting={true} textComponent={scoreboard.header}/>
            </div>
        {/if}
        <div class="entries">
            {#each scoreboard.entries as {name, score}, i}
                <div class="row">
                    {#if i === scoreboard.entries.length - 1 && $scoreboardIP}
                        <div class="ip-address">
                            <GradientAnimatedText text={$scoreboardIP}/>
                        </div>
                    {:else}
                        <TextComponent fontSize={16} allowPreformatting={true} textComponent={name}/>
                    {/if}
                    <div class="score" class:hidden={!settings?.numbers}>
                        <TextComponent fontSize={16} allowPreformatting={true} textComponent={score}/>
                    </div>
                </div>
            {/each}
        </div>
    </div>
{/if}


<style lang="scss">
  @use "sass:color";
  @use "../../../colors.scss" as *;

  :root {
    --primary-color-rgb: var(--primary-color-rgb);
    --secondary-color-rgb: var(--secondary-color-rgb);
  }

  .scoreboard {
    position: relative;
    display: inline-block;
    width: max-content;
    max-width: 240px;
    right: 0;
    transition: width 0.2s ease;
    transform: translateX(0);
    font-family: 'Alibaba', sans-serif;
  }

  .entries {
    padding: 10px;
    position: relative;
    z-index: 1;
  }

  .row {
    display: flex;
    column-gap: 15px;
    gap: 4px;
    justify-content: space-between;
    white-space: nowrap;
    align-items: center;
  }

  .header {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 10px;
    text-shadow: 0 0 5px rgba($text-color, 0.4),
    1px 1px 1px rgba(color.scale($scoreboard-base-color, $lightness: -20%), 0.6);
    text-align: center;
    white-space: nowrap;
    box-sizing: border-box;
  }

  .ip-address {
    text-align: left;
    flex-grow: 1;
  }

  .hidden {
    visibility: hidden;
    opacity: 0;
  }
</style>
