<script lang="ts">
    import {onMount} from "svelte";
    import {getConnectionDetails, getGameWindow} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import type {ConnectionDetails, GameWindow} from "../../../integration/types";
    import type {ConnectionDetailsEvent} from "../../../integration/events";

    interface Part {
        text: string;
        value?: boolean;
    }

    let details: ConnectionDetails | null = $state(null);
    let gameWindow: GameWindow | null = $state(null);
    let innerHeight = $state(0);

    // One GUI pixel and the GUI row under the status text, which the client draws at height / 2 - 80
    const guiPixel = $derived(gameWindow ? innerHeight / gameWindow.height * gameWindow.scaleFactor : 0);
    const top = $derived(gameWindow ? (Math.floor(gameWindow.scaledHeight / 2) - 62) * guiPixel : 0);
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

    async function updateGameWindow() {
        gameWindow = await getGameWindow();
    }

    listen("scaleFactorChange", updateGameWindow);

    listen("connectionDetails", (event: ConnectionDetailsEvent) => {
        details = event.details;
    });

    onMount(async () => {
        await updateGameWindow();
        const current = await getConnectionDetails();
        // An event received while fetching is newer
        details ??= current;
    });
</script>

<svelte:window bind:innerHeight onresize={updateGameWindow}/>

{#if details && gameWindow}
    <div class="connection-details" style="top: {top}px; font-size: {Math.max(12, 4 * guiPixel)}px">
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
