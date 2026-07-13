<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {ClosedCaptionEntry, ClosedCaptionsEvent} from "../../../integration/events";
    import TextComponent from "../../menu/common/TextComponent.svelte";

    let entries: ClosedCaptionEntry[] = [];

    listen("closedCaptions", (event: ClosedCaptionsEvent) => {
        entries = event.entries;
    });

    function argbToCss(color: number): string {
        const unsigned = color >>> 0;
        const alpha = ((unsigned >>> 24) & 0xff) / 255;
        const red = (unsigned >>> 16) & 0xff;
        const green = (unsigned >>> 8) & 0xff;
        const blue = unsigned & 0xff;

        return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
    }

    function argbToHex(color: number): string {
        return `#${(color & 0xffffff).toString(16).padStart(6, "0")}`;
    }
</script>

{#if entries.length > 0}
    <div class="closed-captions">
        {#each entries as entry}
            <div
                class="closed-caption-entry"
                style:color={argbToCss(entry.textColor)}
                style:background-color={argbToCss(entry.backgroundColor)}
            >
                {#if entry.direction === "LEFT"}
                    <span class="arrow">&lt;</span>
                {:else if entry.direction === "RIGHT"}
                    <span class="arrow">&gt;</span>
                {/if}
                <span class="text">
                    {#if typeof entry.text === "string"}
                        {entry.text}
                    {:else}
                        <TextComponent
                            fontSize={14}
                            textComponent={entry.text}
                            inheritedColor={argbToHex(entry.textColor)}
                        />
                    {/if}
                </span>
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">
    .closed-captions {
        display: flex;
        flex-direction: column-reverse;
        align-items: flex-end;
        gap: 2px;
    }

    .closed-caption-entry {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 2px 6px;
        border-radius: 2px;
        font-weight: 500;
        font-size: 14px;
    }

    .arrow {
        font-weight: bold;
    }
</style>
