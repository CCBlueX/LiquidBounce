<script lang="ts" context="module">
    let highestEditorZIndex = 0;
</script>

<script lang="ts">
    import {getContext, onMount, tick} from "svelte";

    import type {KeyboardKeyEvent, ScaleFactorChangeEvent} from "../../../integration/events";
    import {getGameWindow, setComponentAlignment} from "../../../integration/rest";
    import {type Alignment, HorizontalAlignment, VerticalAlignment} from "../../../integration/types.js";
    import {listen} from "../../../integration/ws";
    import ComponentSettings from "../../clickgui/tabs/hud_editor/ComponentSettings.svelte";
    import {
        type HorizontalAnchorZone,
        HUD_EDITOR_ELEMENTS_CONTEXT,
        HUD_EDITOR_GRID_SIZE,
        HUD_EDITOR_MAGNET_THRESHOLD,
        type HudEditorDragState,
        type VerticalAnchorZone
    } from "../../clickgui/tabs/hud_editor/constants";
    import {fade, type TransitionConfig} from "svelte/transition";

    export let alignment: Alignment;
    export let componentId: string;
    export let componentName: string;
    export let inEditor: boolean;
    export let onDragStateChange: ((state: HudEditorDragState) => void) | undefined = undefined;
    export let magneticallyReferenced = false;
    export let width: number | undefined = undefined;
    export let height: number | undefined = undefined;

    let scaleFactor = 2;
    let element: HTMLElement | undefined;
    let isDragging = false;
    let isGridIgnored = false;
    let pointerCenterOffsetX = 0;
    let pointerCenterOffsetY = 0;
    let horizontalZone: HorizontalAnchorZone = "left";
    let verticalZone: VerticalAnchorZone = "upper";
    let verticalGuide: number | undefined;
    let horizontalGuide: number | undefined;
    let horizontalTargetId: string | undefined;
    let verticalTargetId: string | undefined;
    let editorZIndex = 0;

    let displayPosition = {
        x: 0,
        y: 0
    };
    let positionOnTop = false;

    const POSITION_OVERLAY_OFFSET = 19;

    const editorElements = getContext<Map<string, HTMLElement>>(HUD_EDITOR_ELEMENTS_CONTEXT);

    $: styleString = generateStyleString(alignment);
    $: sizeStyleString = (width !== undefined && height !== undefined)
        ? `width: ${width}px; height: ${height}px;`
        : "";

    function clamp(value: number, min: number, max: number): number {
        return Math.max(min, Math.min(value, max));
    }

    function toHudCoordinate(clientCoordinate: number): number {
        return clientCoordinate * (2 / scaleFactor);
    }

    function getHudWidth(): number {
        return toHudCoordinate(window.innerWidth);
    }

    function getHudHeight(): number {
        return toHudCoordinate(window.innerHeight);
    }

    function getElementWidth(): number {
        return toHudCoordinate(element?.getBoundingClientRect().width ?? 0);
    }

    function getElementHeight(): number {
        return toHudCoordinate(element?.getBoundingClientRect().height ?? 0);
    }

    function getHorizontalCenter(): number {
        const elementWidth = getElementWidth();
        const hudWidth = getHudWidth();

        switch (alignment.horizontalAlignment) {
            case HorizontalAlignment.LEFT:
                return alignment.horizontalOffset + elementWidth / 2;
            case HorizontalAlignment.RIGHT:
                return hudWidth - alignment.horizontalOffset - elementWidth / 2;
            case HorizontalAlignment.CENTER:
                return hudWidth / 2 + alignment.horizontalOffset + elementWidth / 2;
            case HorizontalAlignment.CENTER_TRANSLATED:
                return hudWidth / 2 + alignment.horizontalOffset;
        }
    }

    function getVerticalCenter(): number {
        const elementHeight = getElementHeight();
        const hudHeight = getHudHeight();

        switch (alignment.verticalAlignment) {
            case VerticalAlignment.TOP:
                return alignment.verticalOffset + elementHeight / 2;
            case VerticalAlignment.BOTTOM:
                return hudHeight - alignment.verticalOffset - elementHeight / 2;
            case VerticalAlignment.CENTER:
                return hudHeight / 2 + alignment.verticalOffset + elementHeight / 2;
            case VerticalAlignment.CENTER_TRANSLATED:
                return hudHeight / 2 + alignment.verticalOffset;
        }
    }

    function getHorizontalZone(cursorX: number): HorizontalAnchorZone {
        const hudWidth = getHudWidth();

        if (cursorX < hudWidth / 3) {
            return "left";
        }
        if (cursorX > hudWidth * 2 / 3) {
            return "right";
        }
        return "center";
    }

    function getVerticalZone(cursorY: number): VerticalAnchorZone {
        const hudHeight = getHudHeight();

        if (cursorY < hudHeight / 3) {
            return "upper";
        }
        if (cursorY > hudHeight * 2 / 3) {
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
        const elementWidth = getElementWidth();
        const hudWidth = getHudWidth();

        switch (anchor) {
            case HorizontalAlignment.LEFT:
                return center - elementWidth / 2;
            case HorizontalAlignment.RIGHT:
                return hudWidth - center - elementWidth / 2;
            case HorizontalAlignment.CENTER:
                return center - hudWidth / 2 - elementWidth / 2;
            case HorizontalAlignment.CENTER_TRANSLATED:
                return center - hudWidth / 2;
        }
    }

    function getVerticalOffset(center: number, anchor: VerticalAlignment): number {
        const elementHeight = getElementHeight();
        const hudHeight = getHudHeight();

        switch (anchor) {
            case VerticalAlignment.TOP:
                return center - elementHeight / 2;
            case VerticalAlignment.BOTTOM:
                return hudHeight - center - elementHeight / 2;
            case VerticalAlignment.CENTER:
                return center - hudHeight / 2 - elementHeight / 2;
            case VerticalAlignment.CENTER_TRANSLATED:
                return center - hudHeight / 2;
        }
    }

    interface MagneticSnap {
        center: number;
        guide: number;
        targetId?: string;
    }

    function emitDragState(dragging: boolean): void {
        onDragStateChange?.({
            dragging,
            horizontalZone,
            verticalZone,
            verticalGuide,
            horizontalGuide,
            magneticTargetIds: [...new Set([horizontalTargetId, verticalTargetId].filter(id => id !== undefined))],
        });
    }

    function updateDragState(
        nextHorizontalZone: HorizontalAnchorZone,
        nextVerticalZone: VerticalAnchorZone,
        nextVerticalGuide?: number,
        nextHorizontalGuide?: number,
        nextHorizontalTargetId?: string,
        nextVerticalTargetId?: string,
    ): void {
        if (horizontalZone === nextHorizontalZone &&
            verticalZone === nextVerticalZone &&
            verticalGuide === nextVerticalGuide &&
            horizontalGuide === nextHorizontalGuide &&
            horizontalTargetId === nextHorizontalTargetId &&
            verticalTargetId === nextVerticalTargetId) {
            return;
        }

        horizontalZone = nextHorizontalZone;
        verticalZone = nextVerticalZone;
        verticalGuide = nextVerticalGuide;
        horizontalGuide = nextHorizontalGuide;
        horizontalTargetId = nextHorizontalTargetId;
        verticalTargetId = nextVerticalTargetId;
        emitDragState(true);
    }

    function getElementPoints(target: HTMLElement, horizontal: boolean): number[] {
        const bounds = target.getBoundingClientRect();
        const start = toHudCoordinate(horizontal ? bounds.left : bounds.top);
        const size = toHudCoordinate(horizontal ? bounds.width : bounds.height);

        return [start, start + size / 2, start + size];
    }

    function findMagneticSnap(center: number, size: number, horizontal: boolean): MagneticSnap | undefined {
        if (isGridIgnored) {
            return undefined;
        }

        const draggedPoints = [center - size / 2, center, center + size / 2];
        const viewportSize = horizontal ? getHudWidth() : getHudHeight();
        const viewportCenter = viewportSize / 2;
        const viewportCenterDistance = viewportCenter - center;
        let closestSnap: MagneticSnap | undefined = Math.abs(viewportCenterDistance) <= HUD_EDITOR_MAGNET_THRESHOLD
            ? {center: viewportCenter, guide: viewportCenter}
            : undefined;
        let closestDistance = closestSnap
            ? Math.abs(viewportCenterDistance)
            : HUD_EDITOR_MAGNET_THRESHOLD + 1;

        for (const [id, target] of editorElements) {
            if (id === componentId) {
                continue;
            }

            for (const targetPoint of getElementPoints(target, horizontal)) {
                for (const draggedPoint of draggedPoints) {
                    const distance = targetPoint - draggedPoint;
                    const snappedCenter = center + distance;

                    if (Math.abs(distance) > HUD_EDITOR_MAGNET_THRESHOLD ||
                        Math.abs(distance) >= closestDistance ||
                        snappedCenter - size / 2 < 0 ||
                        snappedCenter + size / 2 > viewportSize) {
                        continue;
                    }

                    closestDistance = Math.abs(distance);
                    closestSnap = {center: snappedCenter, guide: targetPoint, targetId: id};
                }
            }
        }

        return closestSnap;
    }

    function onMouseDown(event: MouseEvent): void {
        if (inEditor) {
            editorZIndex = ++highestEditorZIndex;
        }

        if (event.button !== 0 && event.button !== 1) {
            return;
        }

        isDragging = true;
        const horizontalCenter = getHorizontalCenter();
        const verticalCenter = getVerticalCenter();
        const cursorX = toHudCoordinate(event.clientX);
        const cursorY = toHudCoordinate(event.clientY);

        pointerCenterOffsetX = horizontalCenter - cursorX;
        pointerCenterOffsetY = verticalCenter - cursorY;
        horizontalZone = getHorizontalZone(cursorX);
        verticalZone = getVerticalZone(cursorY);
        verticalGuide = undefined;
        horizontalGuide = undefined;
        horizontalTargetId = undefined;
        verticalTargetId = undefined;
        void updateDisplayedPosition();
        emitDragState(true);
    }

    async function updateDisplayedPosition(): Promise<void> {
        await tick();

        if (!element) {
            return;
        }

        const bounds = element.getBoundingClientRect();
        displayPosition = {
            x: Math.round(bounds.x),
            y: Math.round(bounds.y)
        };
        positionOnTop = bounds.top + bounds.height / 2 >= window.innerHeight / 2;
    }

    function onMouseMove(event: MouseEvent): void {
        if (!isDragging) {
            return;
        }

        const cursorX = toHudCoordinate(event.clientX);
        const cursorY = toHudCoordinate(event.clientY);
        const horizontalCenter = cursorX + pointerCenterOffsetX;
        const verticalCenter = cursorY + pointerCenterOffsetY;
        const nextHorizontalZone = getHorizontalZone(cursorX);
        const nextVerticalZone = getVerticalZone(cursorY);
        const elementWidth = getElementWidth();
        const elementHeight = getElementHeight();
        const horizontalSnap = findMagneticSnap(horizontalCenter, elementWidth, true);
        const verticalSnap = findMagneticSnap(verticalCenter, elementHeight, false);

        alignment.horizontalAlignment = getHorizontalAlignment(nextHorizontalZone);
        alignment.verticalAlignment = getVerticalAlignment(nextVerticalZone);

        const horizontalOffset = getHorizontalOffset(
            horizontalSnap?.center ?? horizontalCenter,
            alignment.horizontalAlignment
        );
        const verticalOffset = getVerticalOffset(
            verticalSnap?.center ?? verticalCenter,
            alignment.verticalAlignment
        );

        alignment.horizontalOffset = clampHorizontalOffset(
            horizontalSnap ? horizontalOffset : snapToGrid(horizontalOffset)
        );
        alignment.verticalOffset = clampVerticalOffset(
            verticalSnap ? verticalOffset : snapToGrid(verticalOffset)
        );

        updateDragState(
            nextHorizontalZone,
            nextVerticalZone,
            horizontalSnap?.guide,
            verticalSnap?.guide,
            horizontalSnap?.targetId,
            verticalSnap?.targetId,
        );
        void updateDisplayedPosition();
    }

    function clampHorizontalOffset(offset: number): number {
        const elementWidth = getElementWidth();
        const hudWidth = getHudWidth();

        switch (alignment.horizontalAlignment) {
            case HorizontalAlignment.CENTER_TRANSLATED:
                return clamp(
                    offset,
                    -hudWidth / 2 + elementWidth / 2,
                    hudWidth / 2 - elementWidth / 2
                );
            case HorizontalAlignment.CENTER:
                return clamp(
                    offset,
                    -hudWidth / 2,
                    hudWidth / 2 - elementWidth
                );
            case HorizontalAlignment.LEFT:
            case HorizontalAlignment.RIGHT:
                return clamp(offset, 0, hudWidth - elementWidth);
        }
    }

    function clampVerticalOffset(offset: number): number {
        const elementHeight = getElementHeight();
        const hudHeight = getHudHeight();

        switch (alignment.verticalAlignment) {
            case VerticalAlignment.CENTER_TRANSLATED:
                return clamp(
                    offset,
                    -hudHeight / 2 + elementHeight / 2,
                    hudHeight / 2 - elementHeight / 2
                );
            case VerticalAlignment.CENTER:
                return clamp(
                    offset,
                    -hudHeight / 2,
                    hudHeight / 2 - elementHeight
                );
            case VerticalAlignment.TOP:
            case VerticalAlignment.BOTTOM:
                return clamp(offset, 0, hudHeight - elementHeight);
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
        verticalGuide = undefined;
        horizontalGuide = undefined;
        horizontalTargetId = undefined;
        verticalTargetId = undefined;
        emitDragState(false);
        setComponentAlignment(componentId, alignment);
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

    function editorFade(node: Element): TransitionConfig {
        return fade(node, {
            duration: inEditor ? 200 : 0
        });
    }

    listen("keyboardKey", (e: KeyboardKeyEvent) => {
        if (e.key === "key.keyboard.left.shift") {
            isGridIgnored = e.action === 1;
        }
    });

    onMount(() => {
        if (!inEditor || !element) {
            return;
        }

        editorElements.set(componentId, element);
        return () => editorElements.delete(componentId);
    });

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
/>

<div class="draggable-element" style="{styleString} z-index: {editorZIndex};" bind:this={element}
     transition:editorFade|global>
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div
            class="contained-element"
            style={sizeStyleString}
            class:editor-mode={inEditor}
            class:magnetically-referenced={inEditor && magneticallyReferenced}
            on:mousedown={onMouseDown}
    >
        <slot/>
    </div>
    {#if isDragging}
        <div class="position" class:top={positionOnTop} transition:fade={{duration: 100}}>
            {displayPosition.x} &#215; {displayPosition.y}
        </div>
    {/if}
    {#if inEditor}
        <ComponentSettings
                name={componentName}
                id={componentId}
                {alignment}
                overlayOffset={isDragging ? POSITION_OVERLAY_OFFSET : 0}
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
        transition: background-color 100ms ease;
    }

    .magnetically-referenced {
        background-color: var(--clickgui-hud-editor-magnetic-reference-background-color);
    }

    .position {
        position: absolute;
        top: calc(100% + 5px);
        left: 0;
        width: max-content;
        height: 14px;
        color: var(--clickgui-text-dimmed-color);
        font-size: 12px;
        text-wrap: nowrap;
        outline: solid 1px var(--clickgui-hud-editor-draggable-element-position-outline-color);
        background-color: var(--clickgui-hud-editor-draggable-element-position-background-color);
    }

    .position.top {
        top: auto;
        bottom: calc(100% + 5px);
    }
</style>
