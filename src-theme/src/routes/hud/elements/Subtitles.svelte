<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {SoundSubtitlesEvent, SoundSubtitleEntry} from "../../../integration/events";

    let subtitles: SoundSubtitleEntry[] = [];

    listen("soundSubtitles", (event: SoundSubtitlesEvent) => {
        subtitles = event.subtitles;
    });
</script>

{#if subtitles.length > 0}
    <div class="subtitles">
        {#each subtitles as entry}
            <div class="subtitle-entry" style="opacity: {entry.opacity}">
                {#if entry.direction === "LEFT"}
                    <span class="arrow">&lt;</span>
                {:else if entry.direction === "RIGHT"}
                    <span class="arrow">&gt;</span>
                {/if}
                <span class="text">
                    {#if typeof entry.text === "string"}
                        {entry.text}
                    {:else}
                        {entry.text.text}
                    {/if}
                </span>
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">
    .subtitles {
        display: flex;
        flex-direction: column-reverse;
        align-items: flex-end;
        gap: 2px;
    }

    .subtitle-entry {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 2px 6px;
        background-color: rgba(0, 0, 0, 0.8);
        border-radius: 2px;
        font-weight: 500;
        font-size: 14px;
        color: var(--subtitle-text-color, #ffffff);
    }

    .arrow {
        font-weight: bold;
        color: var(--subtitle-arrow-color, #ffcc00);
    }
</style>
