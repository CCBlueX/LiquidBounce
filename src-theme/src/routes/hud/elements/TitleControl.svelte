<script lang="ts">
    import {fade} from "svelte/transition";
    import {listen} from "../../../integration/ws";
    import type {OverlayTitleEvent} from "../../../integration/events";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import {Interval} from "../../../util/timeout_utils";

    let OverlayTitle: OverlayTitleEvent | null = null;
    const timeouts = new Interval();
    const OVERLAY_TIMEOUT = 3000;

    listen("overlayTitle", (event: OverlayTitleEvent) => {
        OverlayTitle = event;
        timeouts.set("overlay", () => OverlayTitle = null, OVERLAY_TIMEOUT);
    });

</script>
<div class="overlay-container">
    {#if OverlayTitle}
        <div class="overlay-message" in:fade={{ duration: 100 }} out:fade={{ duration: 100 }}>
            <div class="title-wrapper">
                {#if OverlayTitle.title}
                    <TextComponent fontSize={50} textComponent={OverlayTitle.title}/>
                {/if}
            </div>

            <div class="subtitle-wrapper">
                {#if OverlayTitle.subtitle}
                    <TextComponent fontSize={36} textComponent={OverlayTitle.subtitle}/>
                {/if}
            </div>
        </div>
    {/if}
</div>


<style lang="scss">
  @use "../../../colors.scss" as *;

  .overlay-container {
    position: fixed;
    top: 0;
    left: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    pointer-events: none;
    width: 100%;
  }

  .overlay-message {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    text-shadow: 1px 1px 2px rgba($base, 0.7);
  }

  .title-wrapper {
    white-space: nowrap;
    padding: 5px;
    font-variant-numeric: tabular-nums;
    min-height: calc(50px + 5px * 2);
    margin-bottom: 30px;
  }

  .subtitle-wrapper {
    white-space: nowrap;
    padding: 5px;
    font-variant-numeric: tabular-nums;
    min-height: calc(36px + 5px * 2);
  }
</style>
