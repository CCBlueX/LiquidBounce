<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {PlayerData, Scoreboard, TextComponent as TTextComponent} from "../../../integration/types";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import {createGlobalRegex, replaceTextComponent} from "../../../util/regex_replace";

    export let settings: { [name: string]: any };

    let cSettings: HudScoreboardSettings;

    $: cSettings = settings as HudScoreboardSettings;

    let scoreboard: Scoreboard | null = null;
    let replaceRegex: RegExp | null = null;
    let processedScoreboard: Scoreboard | null = null;

    $: replaceRegex = createGlobalRegex(cSettings?.replaceRegex);
    $: processedScoreboard = processScoreboard(scoreboard, replaceRegex, cSettings?.replaceWith ?? "");

    function processScoreboard(scoreboard: Scoreboard | null, regex: RegExp | null, replacement: string): Scoreboard | null {
        if (!scoreboard || !regex) {
            return scoreboard;
        }

        return {
            header: replaceTextComponent(scoreboard.header, regex, replacement) as TTextComponent,
            entries: scoreboard.entries.map(({name, score}) => ({
                name: replaceTextComponent(name, regex, replacement) as TTextComponent,
                score: replaceTextComponent(score, regex, replacement) as TTextComponent
            }))
        };
    }

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        const playerData: PlayerData = e.playerData;
        scoreboard = playerData.scoreboard;
    });
</script>

{#if processedScoreboard}
    <div class="scoreboard">
        {#if processedScoreboard.header && cSettings.show.includes('Header')}
            <div class="header">
                <TextComponent fontSize={14} allowPreformatting={true} textComponent={processedScoreboard.header}/>
            </div>
        {/if}
        <div class="entries">
            {#each processedScoreboard.entries as {name, score}}
                <div class="row">
                    {#if cSettings.show.includes('Name')}
                        <TextComponent fontSize={14} allowPreformatting={true} textComponent={name}/>
                    {/if}
                    {#if cSettings.show.includes('Score')}
                        <TextComponent fontSize={14} allowPreformatting={true} textComponent={score}/>
                    {/if}
                </div>
            {/each}
        </div>
    </div>
{/if}

<style lang="scss">

  .scoreboard {
    width: max-content;
    border-radius: 5px;
    overflow: hidden;
    font-size: 14px;
  }

  .entries {
    background-color: var(--scoreboard-body-background-color);
    padding: 10px;
  }

  .row {
    display: flex;
    column-gap: 15px;
    justify-content: space-between;
  }

  .header {
    text-align: center;
    background-color: var(--scoreboard-header-background-color);
    padding: 7px 10px;
  }
</style>
