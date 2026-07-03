<script lang="ts">
    import {onMount} from "svelte";

    import type {ScaleFactorChangeEvent} from "../../../integration/events";
    import {getGameWindow, setComponentAlignment} from "../../../integration/rest";
    import {type Alignment, HorizontalAlignment, VerticalAlignment} from "../../../integration/types.js";
    import {listen} from "../../../integration/ws";
    import ComponentSettings from "../../clickgui/tabs/hud_editor/ComponentSettings.svelte";
    import {
        type HorizontalAnchorZone,
        HUD_EDITOR_GRID_SIZE,
        type HudEditorDragState,
        type VerticalAnchorZone
    } from "../../clickgui/tabs/hud_editor/constants";

    export let alignment: Alignment;
    export let componentId: string;
    export let componentName: string;
    export let inEditor: boolean;
    export let onDragStateChange: ((state: HudEditorDragState) => void) | undefined = undefined;

    // TODO: Use correct scale factor
    let scaleFactor = 2;
    let element: HTMLElement | undefined;
    let isDragging = false;
    let isGridIgnored = false;
    let pointerCenterOffsetX = 0;
    let pointerCenterOffsetY = 0;
    let horizontalZone: HorizontalAnchorZone = "left";
    let verticalZone: VerticalAnchorZone = "upper";

    $: styleString = generateStyleString(alignment);

    function clamp(value: number, min: number, max: number): number {
        return Math.max(min, Math.min(value, max));
    }

    function toHudCoordinate(clientCoordinate: number): number {
        return clientCoordinate * (2 / scaleFactor);
    }

    function getHorizontalCenter(): number {
        const elementWidth = element?.offsetWidth ?? 0;

        switch (alignment.horizontalAlignment) {
            case HorizontalAlignment.LEFT:
                return alignment.horizontalOffset + elementWidth / 2;
            case HorizontalAlignment.RIGHT:
                return window.innerWidth - alignment.horizontalOffset - elementWidth / 2;
            case HorizontalAlignment.CENTER:
                return window.innerWidth / 2 + alignment.horizontalOffset + elementWidth / 2;
            case HorizontalAlignment.CENTER_TRANSLATED:
                return window.innerWidth / 2 + alignment.horizontalOffset;
        }
    }

    function getVerticalCenter(): number {
        const elementHeight = element?.offsetHeight ?? 0;

        switch (alignment.verticalAlignment) {
            case VerticalAlignment.TOP:
                return alignment.verticalOffset + elementHeight / 2;
            case VerticalAlignment.BOTTOM:
                return window.innerHeight - alignment.verticalOffset - elementHeight / 2;
            case VerticalAlignment.CENTER:
                return window.innerHeight / 2 + alignment.verticalOffset + elementHeight / 2;
            case VerticalAlignment.CENTER_TRANSLATED:
                return window.innerHeight / 2 + alignment.verticalOffset;
        }
    }

    function getHorizontalZone(center: number): HorizontalAnchorZone {
        if (center < window.innerWidth / 3) {
            return "left";
        }
        if (center > window.innerWidth * 2 / 3) {
            return "right";
        }
        return "center";
    }

    function getVerticalZone(center: number): VerticalAnchorZone {
        if (center < window.innerHeight / 3) {
            return "upper";
        }
        if (center > window.innerHeight * 2 / 3) {
            return "lower";
        }
        return "center";
    }

    function getHorizontalAlignment(zone: HorizontalAnchorZone): HorizontalAlignment {
        switch (zone) {
            case "left":
                return HorizontalAlignment.LEFT;
            case "center":
                return HorizontalAlignment.CENTER_TRANSLATED;
            case "right":
                return HorizontalAlignment.RIGHT;
        }
    }

    function getVerticalAlignment(zone: VerticalAnchorZone): VerticalAlignment {
        switch (zone) {
            case "upper":
                return VerticalAlignment.TOP;
            case "center":
                return VerticalAlignment.CENTER_TRANSLATED;
            case "lower":
                return VerticalAlignment.BOTTOM;
        }
    }

    function getHorizontalOffset(center: number, anchor: HorizontalAlignment): number {
        const elementWidth = element?.offsetWidth ?? 0;

        switch (anchor) {
            case HorizontalAlignment.LEFT:
                return center - elementWidth / 2;
            case HorizontalAlignment.RIGHT:
                return window.innerWidth - center - elementWidth / 2;
            case HorizontalAlignment.CENTER:
                return center - window.innerWidth / 2 - elementWidth / 2;
            case HorizontalAlignment.CENTER_TRANSLATED:
                return center - window.innerWidth / 2;
        }
    }

    function getVerticalOffset(center: number, anchor: VerticalAlignment): number {
        const elementHeight = element?.offsetHeight ?? 0;

        switch (anchor) {
            case VerticalAlignment.TOP:
                return center - elementHeight / 2;
            case VerticalAlignment.BOTTOM:
                return window.innerHeight - center - elementHeight / 2;
            case VerticalAlignment.CENTER:
                return center - window.innerHeight / 2 - elementHeight / 2;
            case VerticalAlignment.CENTER_TRANSLATED:
                return center - window.innerHeight / 2;
        }
    }

    function emitDragState(dragging: boolean): void {
        onDragStateChange?.({dragging, horizontalZone, verticalZone});
    }

    function onMouseDown(event: MouseEvent): void {
        if (event.button !== 0 && event.button !== 1) {
            return;
        }

        isDragging = true;
        const horizontalCenter = getHorizontalCenter();
        const verticalCenter = getVerticalCenter();

        pointerCenterOffsetX = horizontalCenter - toHudCoordinate(event.clientX);
        pointerCenterOffsetY = verticalCenter - toHudCoordinate(event.clientY);
        horizontalZone = getHorizontalZone(horizontalCenter);
        verticalZone = getVerticalZone(verticalCenter);
        emitDragState(true);
    }

    function onMouseMove(event: MouseEvent): void {
        if (!isDragging) {
            return;
        }

        const horizontalCenter = toHudCoordinate(event.clientX) + pointerCenterOffsetX;
        const verticalCenter = toHudCoordinate(event.clientY) + pointerCenterOffsetY;
        const nextHorizontalZone = getHorizontalZone(horizontalCenter);
        const nextVerticalZone = getVerticalZone(verticalCenter);

        alignment.horizontalAlignment = getHorizontalAlignment(nextHorizontalZone);
        alignment.verticalAlignment = getVerticalAlignment(nextVerticalZone);

        const horizontalOffset = snapToGrid(getHorizontalOffset(horizontalCenter, alignment.horizontalAlignment));
        const verticalOffset = snapToGrid(getVerticalOffset(verticalCenter, alignment.verticalAlignment));

        alignment.horizontalOffset = clampHorizontalOffset(horizontalOffset);
        alignment.verticalOffset = clampVerticalOffset(verticalOffset);

        if (horizontalZone !== nextHorizontalZone || verticalZone !== nextVerticalZone) {
            horizontalZone = nextHorizontalZone;
            verticalZone = nextVerticalZone;
            emitDragState(true);
        }
    }

    function clampHorizontalOffset(offset: number): number {
        const elementWidth = element?.offsetWidth ?? 0;

        switch (alignment.horizontalAlignment) {
            case HorizontalAlignment.CENTER_TRANSLATED:
                return clamp(
                    offset,
                    -window.innerWidth / 2 + elementWidth / 2,
                    window.innerWidth / 2 - elementWidth / 2
                );
            case HorizontalAlignment.CENTER:
                return clamp(
                    offset,
                    -window.innerWidth / 2,
                    window.innerWidth / 2 - elementWidth
                );
            case HorizontalAlignment.LEFT:
            case HorizontalAlignment.RIGHT:
                return clamp(offset, 0, window.innerWidth - elementWidth);
        }
    }

    function clampVerticalOffset(offset: number): number {
        const elementHeight = element?.offsetHeight ?? 0;

        switch (alignment.verticalAlignment) {
            case VerticalAlignment.CENTER_TRANSLATED:
                return clamp(
                    offset,
                    -window.innerHeight / 2 + elementHeight / 2,
                    window.innerHeight / 2 - elementHeight / 2
                );
            case VerticalAlignment.CENTER:
                return clamp(
                    offset,
                    -window.innerHeight / 2,
                    window.innerHeight / 2 - elementHeight
                );
            case VerticalAlignment.TOP:
            case VerticalAlignment.BOTTOM:
                return clamp(offset, 0, window.innerHeight - elementHeight);
        }
    }

    function snapToGrid(value: number): number {
        return isGridIgnored ? value : Math.round(value / HUD_EDITOR_GRID_SIZE) * HUD_EDITOR_GRID_SIZE;
    }

    function onMouseUp(): void {
        if (!isDragging) {
            return;
        }

        isDragging = false;
        emitDragState(false);
        void setComponentAlignment(componentId, alignment);
    }

    function generateStyleString(alignment: Alignment): string {
        const translateX = alignment.horizontalAlignment === HorizontalAlignment.CENTER_TRANSLATED ? "-50%" : "0";
        const translateY = alignment.verticalAlignment === VerticalAlignment.CENTER_TRANSLATED ? "-50%" : "0";

        return [
            "position: fixed;",
            getHorizontalStyle(alignment),
            getVerticalStyle(alignment),
            `transform: translate(${translateX}, ${translateY});`
        ].join(" ");
    }

    function getHorizontalStyle(alignment: Alignment): string {
        switch (alignment.horizontalAlignment) {
            case HorizontalAlignment.LEFT:
                return `left: ${alignment.horizontalOffset}px;`;
            case HorizontalAlignment.RIGHT:
                return `right: ${alignment.horizontalOffset}px;`;
            case HorizontalAlignment.CENTER:
            case HorizontalAlignment.CENTER_TRANSLATED:
                return `left: calc(50% + ${alignment.horizontalOffset}px);`;
        }
    }

    function getVerticalStyle(alignment: Alignment): string {
        switch (alignment.verticalAlignment) {
            case VerticalAlignment.TOP:
                return `top: ${alignment.verticalOffset}px;`;
            case VerticalAlignment.BOTTOM:
                return `bottom: ${alignment.verticalOffset}px;`;
            case VerticalAlignment.CENTER:
            case VerticalAlignment.CENTER_TRANSLATED:
                return `top: calc(50% + ${alignment.verticalOffset}px);`;
        }
    }

    function handleKeydown(event: KeyboardEvent): void {
        if (event.key === "Shift") {
            isGridIgnored = true;
        }
    }

    function handleKeyup(event: KeyboardEvent): void {
        if (event.key === "Shift") {
            isGridIgnored = false;
        }
    }

    onMount(async () => {
        const gameWindow = await getGameWindow();
        scaleFactor = gameWindow.scaleFactor;
    });

    listen("scaleFactorChange", (event: ScaleFactorChangeEvent) => {
        scaleFactor = event.scaleFactor;
    });
</script>

<svelte:window
        on:mouseup={onMouseUp}
        on:mousemove={onMouseMove}
        on:keydown={handleKeydown}
        on:keyup={handleKeyup}
/>

<div class="draggable-element" style={styleString} bind:this={element}>
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="contained-element" on:mousedown={onMouseDown} class:editor-mode={inEditor}>
        <slot/>
    </div>
    {#if inEditor}
        <ComponentSettings
                name={componentName}
                id={componentId}
                {alignment}
        />
    {/if}
</div>

<style>
    .contained-element {
        min-width: 50px;
        min-height: 50px;
    }

    .editor-mode {
        outline: solid 1px var(--clickgui-hud-editor-draggable-element-outline-color);
        background-color: var(--clickgui-hud-editor-draggable-element-background-color);
    }
</style>
