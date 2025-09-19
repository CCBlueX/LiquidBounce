<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {PlayerData, Scoreboard} from "../../../integration/types";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import {intToRgba, rgbaToHex} from "../../../integration/util";

    export let settings: { [name: string]: any };

    let scoreboard: Scoreboard | null = null;

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        const playerData: PlayerData = e.playerData;
        scoreboard = playerData.scoreboard;
    });
</script>

{#if scoreboard}
    <div class="scoreboard">
        {#if scoreboard.header}
            <div class="header">
                <TextComponent fontSize={14} allowPreformatting={true} textComponent={scoreboard.header}/>
            </div>
        {/if}

        <div class="entries">
            {#each scoreboard.entries as {name, score}, i}
                <div class="row">
                    {#if i === scoreboard.entries.length - 1 && settings?.address}
                        <div class="custom-ip" style="
                            text-align: center;
                            color: {rgbaToHex(intToRgba(settings.addressColor))};
                            font-weight: bold;
                            width: 100%;
                        ">
                            {settings.address}
                        </div>
                    {:else}
                        <TextComponent fontSize={14} allowPreformatting={true} textComponent={name}/>
                    {/if}

                    <div class:hidden={!settings?.numbers}>
                        <TextComponent fontSize={14} allowPreformatting={true} textComponent={score}/>
                    </div>
                </div>
            {/each}
        </div>
    </div>
{/if}

<style lang="scss">
  @use "../../../colors.scss" as *;

  .scoreboard {
    width: max-content;
    border-radius: 5px;
    overflow: hidden;
    font-size: 14px;
  }

  .entries {
    background-color: rgba($scoreboard-base-color, 0.5);
    padding: 10px;
  }

  .row {
    display: flex;
    column-gap: 15px;
    justify-content: space-between;
  }

  .header {
    text-align: center;
    background-color: rgba($scoreboard-base-color, 0.68);
    padding: 7px 10px;
  }

  .hidden {
    visibility: hidden;
    opacity: 0;
  }

  .custom-ip {
    display: block;
    text-align: center;
    width: 100%;
  }
</style>

