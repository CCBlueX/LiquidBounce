<script lang="ts">
    import { listen } from "../../../../integration/ws";
    import { fly } from "svelte/transition";
    import { expoInOut } from "svelte/easing";
    import TextComponent from "../../../menu/common/TextComponent.svelte";
    import type { PlayerData, Scoreboard } from "../../../../integration/types";
    import type { ClientPlayerDataEvent } from "../../../../integration/events";
    import { scoreboardIP } from "../../../../util/Theme/ThemeManager";
    import GradientAnimatedText from "../../common/FontRenderer/GradientAnimatedText.svelte";
    import Rise_Arraylist from "./mode/Rise_Arraylist.svelte";
    import Simple_Arraylist from "./mode/Simple_Arraylist.svelte";

    const modes: Record<string, any> = {
        Simple:Simple_Arraylist,
        Rise:Rise_Arraylist,
    }
    export let settings: { [name: string]: any };
    $: ModeComponent = modes[settings?.mode] ??Rise_Arraylist ;
    let scoreboard: Scoreboard | null = null;
    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        const playerData: PlayerData = e.playerData;
        scoreboard = playerData.scoreboard;
    });
</script>

<div class="combined-container">
    <div class="arraylist-section" id="arraylist"
         transition:fly|global={{duration: 500, y: -50, easing: expoInOut}}>
        <svelte:component this={ModeComponent} {settings} />
    </div>

    {#if scoreboard  && settings?.scoreboard}
        <div class="scoreboard-section" transition:fly|global={{duration: 500, x: 50, easing: expoInOut}}>
            {#if scoreboard.header}
                <div class="header">
                    <TextComponent fontSize={18} allowPreformatting={true} textComponent={scoreboard.header} />
                </div>
            {/if}
            <div class="entries">
                {#each scoreboard.entries as {name, score}, i}
                    <div class="row">
                        {#if i === scoreboard.entries.length - 1 && $scoreboardIP}
                            <div class="ip-address">
                                <GradientAnimatedText text={$scoreboardIP} />
                            </div>
                        {:else}
                            <TextComponent fontSize={16} allowPreformatting={true} textComponent={name} />
                        {/if}
                        <div class="invisible-score">
                            <TextComponent fontSize={16} allowPreformatting={true} textComponent={score} />
                        </div>
                    </div>
                {/each}
            </div>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "sass:color";
  @use"../../../../colors.scss" as *;

  :root {
    --primary-color-rgb: var(--primary-color-rgb);
    --secondary-color-rgb: var(--secondary-color-rgb);
  }

  .combined-container {
    top: 0;
    right: 0;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    position: absolute;
    gap: 5px;
  }

  .arraylist-section {
    border-radius: 0;
    overflow: visible;
    margin-bottom: 5px;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    transform: translateZ(0);
  }

  .scoreboard-section {
    position: relative;
    background: linear-gradient(
                    90deg,
                    transparent 0%,
                    rgba(color.adjust($scoreboard-base-color, $lightness: -5%), 0.5) 100%);
    display: inline-block;
    width: max-content;
    max-width: 240px;
    right: 0;
    transition: width 0.2s ease;
    transform: translateX(0);
    font-family: 'Alibaba', sans-serif;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(
                      90deg,
                      rgba($scoreboard-base-color, 0.1) 0%,
                      rgba($scoreboard-base-color, 0.4) 100%);
      z-index: -1;
      filter: blur(10px);
      opacity: 0.7;
      transform: scale(0.95) translateY(5px);
    }
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

  .invisible-score {
    visibility: hidden;
    opacity: 0;
  }
</style>
