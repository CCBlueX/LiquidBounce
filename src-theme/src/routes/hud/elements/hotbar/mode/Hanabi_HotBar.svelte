<script lang="ts">
    import {listen} from "../../../../../integration/ws";
    import type {ClientPlayerDataEvent, ClientPlayerInventoryEvent} from "../../../../../integration/events";
    import type {PlayerData, ItemStack, ClientInfo} from "../../../../../integration/types";
    import {getClientInfo, getPlayerData, getPlayerInventory} from "../../../../../integration/rest";
    import {onMount} from "svelte";
    import ItemIndexView from "../../../common/ItemView/ItemIndexView.svelte";
    import {clientName} from "../../../../../util/Theme/ThemeManager";

    let currentSlot = 0;
    let playerData: PlayerData | null = null;
    let hotbar: ItemStack[] = [];
    let clientInfo: ClientInfo | null = null;
    const slotWidth = 45;
    const slotGap = 5;

    const userData = JSON.parse(
        localStorage.getItem('userSettings') ||
        JSON.stringify({username: 'Customer'})
    );

    function updatePlayerData(s: PlayerData) {
        playerData = s;
        currentSlot = playerData.selectedSlot;
    }

    function updateInventory(inventory: any) {
        hotbar = [...inventory.main.slice(0, 9)];
    }

    async function updateClientInfo() {
        clientInfo = await getClientInfo();
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => updatePlayerData(event.playerData));
    listen("clientPlayerInventory", (event: ClientPlayerInventoryEvent) => updateInventory(event.inventory));

    onMount(async () => {
        await updateClientInfo();
        setInterval(updateClientInfo, 1000);
        updatePlayerData(await getPlayerData());
        updateInventory(await getPlayerInventory());
    });

    function pingColor(ping: number | null) {
        if (!ping) return "hsl(200, 100%, 50%)";
        if (ping <= 50) return "hsl(200, 100%, 50%)";
        if (ping <= 125) return "orange";
        return "red";
    }
</script>

{#if playerData && clientInfo && playerData.gameMode !== "spectator"}
    <div class="hotbar">
        <div class="hotbar-elements">
            <div class="left-info">
                <div class="ping">
                    <span class="ping-dot"
                          style="--dot-color: {pingColor(playerData.ping)}; background-color: {pingColor(playerData.ping)}"></span>
                    PING: {playerData.ping ? `${playerData.ping}ms` : 'N/A'}
                </div>
                <div class="fps">FPS: {clientInfo.fps}</div>
            </div>
            <div class="slots-container">
                <div class="slider" style="left: {(slotWidth + slotGap) * currentSlot}px"></div>
                <div class="slots">
                    {#each hotbar as stack (stack)}
                        <div class="slot">
                            {#if stack && stack.identifier !== "minecraft:air"}
                                <ItemIndexView {stack}/>
                            {/if}
                        </div>
                    {/each}
                </div>
            </div>
            <div class="build-version">
                {$clientName ? $clientName : `JMcomicFix`} Build {clientInfo.clientVersion} Developer Version
                - {userData.username}
            </div>
        </div>
    </div>
{/if}

<style lang="scss">
  @use "../../../../../colors" as *;

  .hotbar {
    position: fixed;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%) scale(1.5);
    width: 100vw;
  }

  .hotbar-elements {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba($base, 0.5);
    padding: 0 5px;

    .left-info {
      position: absolute;
      left: 5px;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .ping, .fps, .build-version {
      color: hsl(0, 0%, 90%);
      text-shadow: 0 0 3px rgba(255, 255, 255, 0.9);
      display: flex;
      align-items: center;
    }

    .build-version {
      position: absolute;
      right: 5px;
    }

    .fps {
      margin-left: 20px;
    }

    .ping-dot {
      display: inline-block;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      margin-right: 5px;
      box-shadow: 0 0 5px var(--dot-color),
      0 0 10px var(--dot-color),
      0 0 15px var(--dot-color);
    }

    .slots-container {
      position: relative;
      display: flex;
      align-items: center;

      .slider {
        position: absolute;
        top: 0;
        width: 45px;
        height: 45px;
        border-radius: 8px;
        background-color: rgba(255, 255, 255, .4);
        transition: left 0.1s ease-in;
        pointer-events: none;
        z-index: 1;
      }

      .slots {
        display: flex;
        gap: 5px;
        position: relative;
        z-index: 2;
      }

      .slot {
        width: 45px;
        height: 45px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }
</style>
