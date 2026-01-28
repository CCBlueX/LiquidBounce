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
    export let title: string;
</script>

<div class="window">
    <div class="title">{title}</div>
    <div class="content">
        <slot />
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .window {
    position: fixed;
    top: 110px;
    left: 50%;
    transform: translateX(-50%);
    width: min(820px, 92vw);
    --window-max-height: 70vh;
    background-color: rgba($clickgui-base-color, 0.9);
    border-radius: 12px;
    box-shadow: 0 0 14px rgba($clickgui-base-color, 0.55);
    overflow: hidden;
    max-height: var(--window-max-height, none);
  }

  .title {
    padding: 16px 22px;
    font-size: 16px;
    font-weight: 600;
    color: $clickgui-text-color;
    border-bottom: 2px solid rgba($accent-color, 0.8);
  }

  .content {
    padding: 12px 22px 18px;
    overflow: auto;
    max-height: calc(var(--window-max-height, 9999px) - 60px);
  }
</style>
