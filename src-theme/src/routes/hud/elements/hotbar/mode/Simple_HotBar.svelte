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

            <div class="hotbar-gradient-line">
                <svg height="4" preserveAspectRatio="none" viewBox="0 0 100 4" width="100%">
                    <defs>
                        <linearGradient id="flow-gradient" x1="0%" x2="100%" y1="0%" y2="0%">
                            <stop offset="0%" stop-color="var(--primary-color)" stop-opacity="0.75"/>
                            <stop offset="50%" stop-color="var(--secondary-color)" stop-opacity="0.75"/>
                            <stop offset="100%" stop-color="var(--primary-color)" stop-opacity="0.75"/>
                            <animateTransform
                                    attributeName="gradientTransform"
                                    dur="4s"
                                    from="1 0"
                                    repeatCount="indefinite"
                                    to="-1 0"
                                    type="translate"
                            />
                        </linearGradient>
                    </defs>
                    <rect fill="url(#flow-gradient)" height="4" width="100%" x="0" y="0"/>
                </svg>
            </div>
        </div>
    </div>
{/if}

<style lang="scss">
  .hotbar-elements {
    position: relative;

    .hotbar-gradient-line {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      pointer-events: none;
      svg {
        display: block;
      }
    }

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
