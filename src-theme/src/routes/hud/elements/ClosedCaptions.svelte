<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {ClosedCaptionEntry, ClosedCaptionsEvent} from "../../../integration/events";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import {intToHex} from "../../../integration/util";

    let entries: ClosedCaptionEntry[] = [];

    listen("closedCaptions", (event: ClosedCaptionsEvent) => {
        entries = event.entries;
    });
</script>

{#if entries.length > 0}
    <div class="closed-captions">
        {#each entries as entry}
            <div
                    class="closed-caption-entry"
                    style:color={intToHex(entry.textColor)}
                    style:background-color={intToHex(entry.backgroundColor)}
            >
                <span class="arrow" class:visible={entry.direction === "LEFT"}>&lt;</span>
                <span class="text">
                    {#if typeof entry.text === "string"}
                        {entry.text}
                    {:else}
                        <TextComponent
                                fontSize={14}
                                textComponent={entry.text}
                                inheritedColor={intToHex(entry.textColor)}
                        />
                    {/if}
                </span>

                <span class="arrow" class:visible={entry.direction === "RIGHT"}>&gt;</span>
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
    visibility: hidden;

    &.visible {
      visibility: visible;
    }
  }
</style>
