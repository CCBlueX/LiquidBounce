<script lang="ts">
    import {getClientInfo, getSession} from "../../../../integration/rest";
    import type {ClientInfo, PlayerData, Session,} from "../../../../integration/types";
    import {onMount} from "svelte";
    import {listen} from "../../../../integration/ws";
    import type {ClientPlayerDataEvent} from "../../../../integration/events";
    import {fly} from 'svelte/transition';
    import {expoInOut} from "svelte/easing";
    import ClientName from "./ClientName.svelte";

    export let settings: { [name: string]: any };

    let clientInfo: ClientInfo | null = null;
    let session: Session | null = null;
    let playerData: PlayerData | null = null;


    let dataPromise = Promise.all([
        getClientInfo().then(info => clientInfo = info),
        getSession().then(s => session = s),
    ]);

    async function updateClientInfo() {
        clientInfo = await getClientInfo();
    }

    async function updateSession() {
        session = await getSession();
    }

    const userData = JSON.parse(
        localStorage.getItem('userSettings') ||
        JSON.stringify({
            username: 'Customer',
        })
    );
    onMount(async () => {
        await dataPromise;
        setInterval(async () => {
            await updateClientInfo();
            await updateSession();
        }, 1000);
    });
    listen("clientPlayerData", ((event: ClientPlayerDataEvent) => {
        playerData = event.playerData;
    }));


    listen("session", async () => {
        await updateSession();
    });
</script>


<div class="watermark hud-container" transition:fly|global={{ duration: 500, y: -50, easing: expoInOut }}>
    <div class="watermark-content">
        {#if clientInfo}
            <ClientName {clientInfo} glow={settings?.glow}/>

            {#if session}
                <div class="separator"></div>
                <div class="info">
                    {userData.username}
                </div>
            {/if}

            {#if playerData}
                <div class="separator"></div>
                <div class="info">{playerData.ping} Ping</div>
                <div class="separator"></div>
                <div class="info">{playerData.serverAddress}</div>
            {/if}
        {/if}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .watermark {
    display: flex;
    white-space: nowrap;
    flex-direction: row;
    align-items: stretch;
    padding: 10px;
    color: hsl(0, 0%, 90%);
    text-shadow: 0 0 3px rgba(255, 255, 255, 0.9);
    font-size: 20px;
    min-width: 150px;
  }

  .watermark-content {
    display: flex;
    align-items: center;
    padding: 0 8px;
  }

  .separator {
    width: 2px;
    height: 16px;
    background: rgba($text, 0.25);
    margin: 0 5px;
  }

  .info {
    margin: 0 6px;
  }
</style>
