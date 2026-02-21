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
    import {type Alignment, HorizontalAlignment, VerticalAlignment} from "../../../integration/types.js";
    import {createEventDispatcher} from "svelte";
    import {scaleFactor, snappingEnabled, gridSize, showGrid} from "../../clickgui/clickgui_store";

    export let alignment: Alignment;
    export let interactive: boolean = false;

    const dispatch = createEventDispatcher();

    $: styleString = generateStyleString(alignment);

    function generateStyleString(alignment: Alignment): string {
        let style = "position: fixed;";

        switch (alignment.horizontalAlignment) {
            case HorizontalAlignment.LEFT:
                style += `left: ${alignment.horizontalOffset}px;`;
                break;
            case HorizontalAlignment.RIGHT:
                style += `right: ${alignment.horizontalOffset}px;`;
                break;
            case HorizontalAlignment.CENTER:
            case HorizontalAlignment.CENTER_TRANSLATED:
                style += `left: calc(50% + ${alignment.horizontalOffset}px);`;
                break;
        }

        switch (alignment.verticalAlignment) {
            case VerticalAlignment.TOP:
                style += `top: ${alignment.verticalOffset}px;`;
                break;
            case VerticalAlignment.BOTTOM:
                style += `bottom: ${alignment.verticalOffset}px;`;
                break;
            case VerticalAlignment.CENTER:
            case VerticalAlignment.CENTER_TRANSLATED:
                style += `top: calc(50% + ${alignment.verticalOffset}px);`;
                break;
        }

        style += "transform: translate("
        if (alignment.horizontalAlignment === HorizontalAlignment.CENTER_TRANSLATED) {
            style += "-50%,";
        } else {
            style += "0,";
        }
        if (alignment.verticalAlignment === VerticalAlignment.CENTER_TRANSLATED) {
            style += "-50%);";
        } else {
            style += "0);"
        }

        return style;
    }

    let dragging = false;
    let startX = 0;
    let startY = 0;
    let startHOffset = 0;
    let startVOffset = 0;

    function onMouseDown(e: MouseEvent) {
        if (!interactive) return;
        if (e.button !== 0) return;

        dragging = true;
        startX = e.clientX;
        startY = e.clientY;
        startHOffset = alignment.horizontalOffset;
        startVOffset = alignment.verticalOffset;
        
        if ($snappingEnabled) {
            $showGrid = true;
        }

        window.addEventListener("mousemove", onMouseMove);
        window.addEventListener("mouseup", onMouseUp);
    }

    function snapToGrid(value: number): number {
        if (!$snappingEnabled) return value;
        return Math.round(value / $gridSize) * $gridSize;
    }

    function onMouseMove(e: MouseEvent) {
        if (!dragging) return;

        const scale = 2 / $scaleFactor;
        const dx = (e.clientX - startX) * scale;
        const dy = (e.clientY - startY) * scale;

        let newHOffset = startHOffset;
        let newVOffset = startVOffset;

        if (alignment.horizontalAlignment === HorizontalAlignment.RIGHT) {
            newHOffset -= dx;
        } else {
            newHOffset += dx;
        }

        if (alignment.verticalAlignment === VerticalAlignment.BOTTOM) {
            newVOffset -= dy;
        } else {
            newVOffset += dy;
        }
        
        newHOffset = snapToGrid(newHOffset);
        newVOffset = snapToGrid(newVOffset);

        alignment = {
            ...alignment,
            horizontalOffset: newHOffset,
            verticalOffset: newVOffset
        };
        
        dispatch("drag", alignment);
    }

    function onMouseUp(e: MouseEvent) {
        if (!dragging) return;
        dragging = false;
        $showGrid = false;
        window.removeEventListener("mousemove", onMouseMove);
        window.removeEventListener("mouseup", onMouseUp);
        
        dispatch("change", alignment);
    }
</script>

<div class="draggable-element" 
     style={styleString} 
     class:interactive={interactive}
     on:mousedown={onMouseDown}
     role="button"
     tabindex="0">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="contained-element">
        <slot/>
    </div>
    {#if interactive}
        <div class="overlay"></div>
    {/if}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .draggable-element {
    position: relative;
    
    &.interactive {
        cursor: move;
        user-select: none;
        
        &:hover .overlay {
            background-color: rgba($clickgui-text-color, 0.2);
            border: 1px solid rgba($clickgui-text-color, 0.5);
        }
    }
  }

  .overlay {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      pointer-events: none;
      z-index: 10;
  }

  .contained-element {
    min-width: 50px;
    min-height: 50px;
  }
</style>
