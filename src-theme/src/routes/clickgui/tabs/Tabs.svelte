<!--
  - This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
  -
  - Copyright (c) 2015 - 2026 CCBlueX
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
    import {activeTab, type ClickGuiTab} from "../clickgui_store";

    const tabs: ClickGuiTab[] = ["Modules", "Settings"];
</script>

<div class="tabs">
    {#each tabs as tab}
        <button
                class="tab"
                class:active={$activeTab === tab}
                on:click={() => activeTab.set(tab)}
        >
            {tab}
        </button>
    {/each}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .tabs {
    position: fixed;
    top: 15px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 8px;
    padding: 6px;
    border-radius: 999px;
    background-color: rgba($clickgui-base-color, 0.85);
    box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
    z-index: 9999999999;
  }

  .tab {
    border: none;
    background: transparent;
    color: $clickgui-text-dimmed-color;
    padding: 6px 14px;
    font-size: 13px;
    font-weight: 600;
    border-radius: 999px;
    cursor: pointer;
    transition: ease background-color 0.2s, ease color 0.2s;

    &:hover {
      color: $clickgui-text-color;
      background-color: rgba($clickgui-base-color, 0.7);
    }

    &.active {
      color: $clickgui-text-color;
      background-color: rgba($accent-color, 0.15);
      border: 1px solid rgba($accent-color, 0.8);
    }
  }
</style>
