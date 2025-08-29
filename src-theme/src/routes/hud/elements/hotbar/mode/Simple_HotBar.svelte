<script lang="ts">
    import {listen} from "../../../../../integration/ws";
    import type {ClientPlayerDataEvent, ClientPlayerInventoryEvent} from "../../../../../integration/events";
    import type {PlayerData, ItemStack} from "../../../../../integration/types";
    import {getPlayerData, getPlayerInventory} from "../../../../../integration/rest";
    import {onMount} from "svelte";
    import ItemIndexView from "../../../common/ItemView/ItemIndexView.svelte";

    let currentSlot = 0;
    let playerData: PlayerData | null = null;
    let hotbar: ItemStack[] = [];


    function updatePlayerData(s: PlayerData) {
        playerData = s;
        currentSlot = playerData.selectedSlot;
    }

    function updateInventory(inventory: any) {
        hotbar = [...inventory.main.slice(0, 9)];
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        updatePlayerData(event.playerData);
    });

    listen("clientPlayerInventory", (event: ClientPlayerInventoryEvent) => {
        updateInventory(event.inventory);
    });

    onMount(async () => {
        updatePlayerData(await getPlayerData());
        updateInventory(await getPlayerInventory());
    });
</script>

{#if playerData && playerData.gameMode !== "spectator"}
    <div class="hotbar">
        <div class="hotbar-elements">
            <div class="slots">
                {#each hotbar as stack, index}
                    <div class="slot" class:selected={index === currentSlot}>
                        {#if stack && stack.identifier !== "minecraft:air"}
                            <ItemIndexView {stack}/>
                        {/if}
                    </div>
                {/each}
            </div>


        </div>
    </div>
{/if}

<style lang="scss">
  .hotbar-elements {
    position: relative;

    .slots {
      display: flex;
    }

    .slot {
      height: 50px;
      width: 45px;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: rgba(0, 0, 0, 0.3);
      transition: background-color 0.1s;

      &.selected {
        background-color: rgba(0, 0, 0, 0.5);
      }
    }
  }
</style>
