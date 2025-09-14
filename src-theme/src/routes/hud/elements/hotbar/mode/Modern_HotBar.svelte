<!--
  - This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
  -
  - Copyright (c) 2015 - 2025 CCBlueX
  -
  - LiquidBounce is free software: you can redistribute it and/or modify
  - it under the terms of the GNU General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - LiquidBounce is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  - GNU General Public License for more details.
  -
  - You should have received a copy of the GNU General Public License
  - along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
  -->

<script lang="ts">
    import {onMount} from "svelte";
    import type {
        ClientPlayerDataEvent,
        PlayerInventory,
        ClientPlayerInventoryEvent
    } from "../../../../../integration/events";
    import type {ItemStack, PlayerData} from "../../../../../integration/types";
    import {listen} from "../../../../../integration/ws";
    import {getPlayerData, getPlayerInventory} from "../../../../../integration/rest";
    import ItemIndexView from "../../../common/ItemView/ItemIndexView.svelte";

    let lastSlot = 0;
    let currentSlot = 0;
    let playerData: PlayerData | null = null;
    let hotbar: ItemStack[] = [];
    let displayedStacks: { index: number, stack: ItemStack }[] = [];

    function updateDisplayedStacks() {
        const stacks = [];
        for (let i = -4; i <= 4; i++) {
            const index = (currentSlot + i + 9) % 9;
            const stack = hotbar[index] ?? null;
            stacks.push({index, stack});
        }
        displayedStacks = stacks;
    }

    function getOpacity(index: number) {
        const diff = Math.abs(index - currentSlot);
        const distance = Math.min(diff, 9 - diff);
        return Math.max(0.3, 0.9 - distance * 0.15);
    }

    function getScale(index: number) {
        return index === currentSlot ? 1.15 : 1.0;
    }

    function updateStacks(inventory: PlayerInventory) {
        hotbar = [...inventory.main.slice(0, 9)];
        updateDisplayedStacks(); // 热栏更新后必须调用
    }

    function updatePlayerData(s: PlayerData) {
        playerData = s;
        currentSlot = playerData.selectedSlot;
        if (currentSlot !== lastSlot) {
            lastSlot = currentSlot;
        }
        updateDisplayedStacks(); // 槽位更新后也得调用
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
        const data = await getPlayerData();
        updatePlayerData(data);
    });
</script>

<div class="hotbar-container">
    <div class="hotbar-track">
        {#each displayedStacks as {index, stack},i (stack)}
            <div
                    class="slot-wrapper"
                    class:active={index === currentSlot}
                    style="
          opacity: {getOpacity(index)};
          --offset: {(i - 4) * 50}px;
          --scale: {getScale(index)};
          transform: translateX(calc(-50% + var(--offset))) scale(var(--scale));
          z-index: {index === currentSlot ? 2 : 1};
        "
            >
                <div class="hud-slot">
                    <div class="slot" class:active={index === currentSlot}>
                        <div class="item-icon">
                            {#if stack}
                                <ItemIndexView stack={stack}/>
                            {/if}
                        </div>
                        {#if index === currentSlot}
                            <div class="selection-overlay"></div>
                        {/if}
                    </div>
                </div>
            </div>
        {/each}
    </div>
</div>


<style lang="scss">
  @use "../../../../../colors" as *;

  ;

  .hotbar-container {
    display: flex;
    justify-content: center;
    padding: 10px;
  }

  .hotbar-track {
    position: relative;
    width: 100%;
    height: 40px;
  }

  .slot-wrapper {
    position: absolute;
    top: 0;
    left: 50%;
    transition: all 0.2s ease-out;
  }

  .slot-wrapper.active {
    filter: drop-shadow(0 0 4px rgba($base, 0.5));
  }

  .hud-slot {
    position: relative;
  }

  .slot {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .item-icon {
    width: 32px;
    height: 32px;
  }

</style>
