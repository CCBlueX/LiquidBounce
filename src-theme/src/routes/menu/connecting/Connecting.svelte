<script lang="ts">
    import {onMount} from "svelte";
    import {getConnectionDetails, getGameWindow} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import type {ConnectionDetails} from "../../../integration/types";
    import type {ConnectionDetailsEvent} from "../../../integration/events";

    interface Part {
        text: string;
        value?: boolean;
    }

    let details: ConnectionDetails | null = $state(null);
    let scaledHeight = $state(0);
    let innerHeight = $state(0);

    // One GUI pixel, so the line sits under the vanilla status text at any GUI scale
    const guiPixel = $derived(innerHeight / scaledHeight);
    const parts = $derived(details ? sentence(details) : []);

    function sentence({remoteAddress, serverAddress, proxy}: ConnectionDetails): Part[] {
        if (proxy) {
            return [
                {text: "Through proxy "},
                {text: remoteAddress, value: true},
                {text: " to "},
                {text: serverAddress, value: true}
            ];
        }

        return [
            {text: "Direct to "},
            {text: serverAddress, value: true},
            {text: ` (${remoteAddress})`}
        ];
    }

    async function updateScaledHeight() {
        scaledHeight = (await getGameWindow()).scaledHeight;
    }

    listen("connectionDetails", (event: ConnectionDetailsEvent) => {
        details = event.details;
    });

    onMount(async () => {
        await updateScaledHeight();
        const current = await getConnectionDetails();
        // An event received while fetching is newer
        details ??= current;
    });
</script>

<svelte:window bind:innerHeight onresize={updateScaledHeight}/>

{#if details && scaledHeight}
    <div class="connection-details"
         style="top: calc(50% - {32 * guiPixel}px); font-size: {Math.max(12, 4 * guiPixel)}px">
        {#each parts as part}<span class:value={part.value}>{part.text}</span>{/each}
    </div>
{/if}

<style lang="scss">
  .connection-details {
    position: fixed;
    left: 0;
    right: 0;
    text-align: center;
    white-space: pre;
    color: var(--menu-text-dimmed-color);
    text-shadow: 0 1px 3px var(--menu-base-68-color);
  }

  .value {
    color: var(--menu-text-color);
    font-weight: 500;
  }
</style>
