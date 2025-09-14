<script lang="ts">
    import {onMount} from "svelte";
    import type {
        ClientPlayerDataEvent,
        PlayerInventory,
        ClientPlayerInventoryEvent
    } from "../../../../integration/events";
    import type {ItemStack, PlayerData} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import {getPlayerData, getPlayerInventory} from "../../../../integration/rest";
    import ItemStackView from "../../common/ItemView/ItemStackView.svelte";
    import {elasticOut} from 'svelte/easing';
    import {Tween} from "svelte/motion";

    let lastSlot = 0;
    let currentSlot = 0;
    let playerData: PlayerData | null = null;
    let hotbar: ItemStack[] = [];
    const glowIntensity = new Tween(0, {
        duration: 300,
        easing: elasticOut
    });

    $: if (currentSlot !== lastSlot) {
        glowIntensity.set(1);
        setTimeout(() => glowIntensity.set(0), 300);
    }

    function updateStacks(inventory: PlayerInventory) {
        hotbar = [...inventory.main.slice(0, 9)];
    }

    function updatePlayerData(s: PlayerData) {
        playerData = s;

        currentSlot = playerData.selectedSlot;
        if (currentSlot !== lastSlot) {
            lastSlot = currentSlot;


        }
    }

    listen("clientPlayerInventory", (data: ClientPlayerInventoryEvent) => {
        updateStacks(data.inventory);
    });
    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        updatePlayerData(event.playerData);
    });
    onMount(async () => {
        const inventory = await getPlayerInventory();
        updateStacks(inventory);
        updatePlayerData(await getPlayerData());
    });


</script>


<div class="hotbar-container hud-container">
    <div class="hotbar-track">
        {#each hotbar as stack, i (stack)}
            <div class="slot-wrapper" class:active={i === currentSlot}>

                <div class="hud-slot">
                    <div class="slot-bg">

                        <div class="slot" class:active={i === currentSlot}>
                            <div class="item-icon">
                                <ItemStackView {stack}/>
                            </div>


                            {#if i === currentSlot}
                                <!-- svelte-ignore element_invalid_self_closing_tag -->
                                <div class="selection-overlay"/>
                            {/if}
                        </div>
                    </div>
                </div>

                <div class="slot-number"
                >
                    {i + 1}
                </div>
            </div>
        {/each}


    </div>
</div>


<style lang="scss">
  @use "../../../../colors" as *;


  .hotbar-container {
    --slot-size: 50px;
    --spacing: 8px;
    position: relative;
    padding: 12px;
    overflow: hidden;
  }

  .selection-overlay {
    position: absolute;
    width: 100%;
    height: 100%;
    background: linear-gradient(
                    135deg,
                    rgba(116, 199, 236, 0.15) 0%,
                    rgba(103, 40, 94, 0.1) 100%
    );
  }


  .hotbar-track {
    display: grid;
    grid-template-columns: repeat(9, 1fr);
    gap: 8px;
    width: 100%;
  }

  .slot-bg {
    width: 100%;
    height: 100%;
    background: linear-gradient(
                    145deg,
                    rgba(30, 25, 40, 0.2) 0%,
                    rgba(20, 15, 30, 0.3) 100%
    );
    border-radius: 12px;
    padding: 2px;
    box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.05),
    0 2px 4px rgba(0, 0, 0, 0.3);

    .active & {
      background: linear-gradient(
                      145deg,
                      rgba(50, 40, 60, 0.2) 0%,
                      rgba(30, 20, 40, 0.3) 100%
      );
    }
  }

  .slot-wrapper {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;

    &.active {
      font-weight: 800;
      text-shadow: 0 0 8px rgba(103, 40, 94, 0.5);

      .slot-number {
        font-weight: bold;
        background-clip: text;
        background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        text-shadow: 0 0 1px color-mix(in srgb, var(--primary-color) 30%, transparent);
      }
    }
  }

  .hud-slot {
    position: relative;
    width: var(--slot-size);
    height: var(--slot-size);
  }

  .slot {
    width: 100%;
    height: 100%;
    display: grid;
    place-items: center;
    background: linear-gradient(
                    135deg,
                    rgba(255, 255, 255, 0.03) 0%,
                    rgba(40, 35, 50, 0.2) 100%
    );
    border-radius: 10px;
    border: 1px solid rgba(255, 255, 255, 0.08);
    position: relative;
    overflow: hidden;

    &.active .item-icon {
      transform: scale(1.15);
      transition: transform 0.1s cubic-bezier(0.4, 0, 0.2, 1);
    }
  }

  .item-icon {
    z-index: 2;
    position: relative;
    background: none;
    transition: transform 0.15s ease;
  }

  .slot-number {
    margin-top: 8px;
    text-align: center;
    font-size: 18px;
    color: #dddddd;
    text-shadow: 0 0 5px currentColor;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
</style>
