<script lang="ts">
    import {onMount} from "svelte";
    import {fade} from "svelte/transition";
    import {listen} from "../../../integration/ws";
    import {getPlayerData} from "../../../integration/rest";
    import type {ClientPlayerDataEvent, OverlayMessageEvent} from "../../../integration/events";
    import type {PlayerData, TextComponent as TTextComponent} from "../../../integration/types";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import {Interval} from "../../../util/timeout_utils";

    let playerData: PlayerData | null = null;
    let overlayMessage: OverlayMessageEvent | null = null;
    let itemStackName: TTextComponent | string | null = null;
    let showItemStackName = false;
    const timeouts = new Interval();
    const ITEM_NAME_TIMEOUT = 2000;
    const OVERLAY_TIMEOUT = 3000;


    function updatePlayerData(newData: PlayerData) {
        const prev = playerData;
        playerData = newData;

        if (prev!.selectedSlot !== newData.selectedSlot) {
            if (newData.mainHandStack?.identifier !== "minecraft:air") {
                itemStackName = newData.mainHandStack?.displayName;
                showItemStackName = true;
                timeouts.set('itemName', () => showItemStackName = false, ITEM_NAME_TIMEOUT);
            }
        }
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        updatePlayerData(event.playerData);
    });
    listen("overlayMessage", (event: OverlayMessageEvent) => {
        overlayMessage = event;
        timeouts.set('overlay', () => overlayMessage = null, OVERLAY_TIMEOUT);
    });
    onMount(async () => {
        updatePlayerData(await getPlayerData());
    });


</script>
<div class="overlay-container">

    {#if playerData && playerData.gameMode !== "spectator" && (overlayMessage || (showItemStackName && itemStackName))}
        <div class="overlay-message" in:fade={{ duration: 100 }} out:fade={{ duration: 200 }}>
            <div class="message">
                {#if overlayMessage}
                    <TextComponent fontSize={20} textComponent={overlayMessage.text}/>
                {/if}
            </div>
            <div class="item-name">
                {#if showItemStackName && itemStackName}
                    <TextComponent fontSize={20} textComponent={itemStackName}/>
                {/if}
            </div>
        </div>
    {/if}
</div>


<style lang="scss">
  @use "../../../colors" as *;

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

  .item-name {
    white-space: nowrap;
    font-variant-numeric: tabular-nums;
    min-height: calc(50px + 5px * 2);
    font-family: "Genshin", sans-serif;
    padding: 5px;
  }

  .message {
    white-space: nowrap;
    min-height: calc(45px + 5px * 2);
    font-variant-numeric: tabular-nums;
    font-family: 'Alibaba', sans-serif;
    font-weight: bold;
    padding: 5px;
    margin-bottom: 5px;
  }
</style>
