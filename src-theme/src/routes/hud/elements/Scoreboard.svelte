<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {PlayerData, Scoreboard} from "../../../integration/types";
    import TextComponent from "../../menu/common/TextComponent.svelte";

    let scoreboard: Scoreboard | null = null;

    listen("clientPlayerData", (e: any) => {
        const playerData: PlayerData = e.playerData;
        scoreboard = playerData.scoreboard;
    });
</script>

{#if scoreboard}
    <div class="scoreboard">
        {#if scoreboard.header}
            <div class="header">
                <TextComponent allowPreformatting={true} textComponent={scoreboard.header}/>
            </div>
        {/if}
        {#each scoreboard.entries as {name, score}}
            <div class="row">
                {#if name}
                    <TextComponent allowPreformatting={true} textComponent={name}/>
                {/if}

                {#if score}
                    <TextComponent allowPreformatting={true} textComponent={score}/>
                {/if}
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">
  @import "../../../colors.scss";

  .scoreboard {
    background-color: rgba($tabgui-base-color, 0.5);
    width: max-content;
    position: fixed;
    left: 15px;
    top: 550px;
    padding: 10px;
    border-radius: 5px;
  }

  .row {
    display: flex;
    column-gap: 15px;
    justify-content: space-between;
  }

  .header {
    text-align: center;
  }
</style>