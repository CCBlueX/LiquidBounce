<script lang="ts">
    import {onMount} from "svelte";
    import {showGrid, snappingEnabled, gridSize} from "./Hud_store";

    export let componentId: string;
    export let defaultPosition = {x: 0, y: 0};
    export let disabled = false;
    export let hudZoom = 100;
    let ignoreGrid = false;


    let position = {...defaultPosition};
    let moving = false;
    let dragStart = {mouseX: 0, mouseY: 0, elemX: 0, elemY: 0};

    onMount(() => {
        const saved = localStorage.getItem(`hud-pos-${componentId}`);
        if (saved) position = JSON.parse(saved);


    });

    function snap(value: number): number {
        if (ignoreGrid || !snappingEnabled) return value;
        return Math.round(value / $gridSize) * $gridSize;
    }

    function handleKeydown(e: KeyboardEvent) {
        if (e.key === "Shift") {
            ignoreGrid = true;
        }
    }

    function handleKeyup(e: KeyboardEvent) {
        if (e.key === "Shift") {
            ignoreGrid = false;
        }
    }

    function savePosition() {
        localStorage.setItem(`hud-pos-${componentId}`, JSON.stringify(position));
    }

    function startDrag(e: MouseEvent) {
        if (disabled || e.button !== 0) return;
        dragStart.mouseX = e.clientX;
        dragStart.mouseY = e.clientY;
        dragStart.elemX = position.x;
        dragStart.elemY = position.y;

        moving = true;
        $showGrid = $snappingEnabled;
        window.addEventListener("mousemove", handleDrag);
        window.addEventListener("mouseup", stopDrag);
    }

    function handleDrag(e: MouseEvent) {
        if (!moving) return;
        const scale = hudZoom / 100;

        let dx = (e.clientX - dragStart.mouseX) / scale;
        let dy = -(e.clientY - dragStart.mouseY) / scale;

        let newX = dragStart.elemX + dx;
        let newY = dragStart.elemY + dy;

        newX = snap(newX);
        newY = snap(newY);


        position = {x: newX, y: newY};
    }

    function stopDrag() {

        if (moving) {
            savePosition();
        }
        moving = false;
        $showGrid = false;
        window.removeEventListener("mousemove", handleDrag);
        window.removeEventListener("mouseup", stopDrag);
    }
</script>
<svelte:window on:keydown={handleKeydown} on:keyup={handleKeyup} on:mousemove={handleDrag} on:mouseup={stopDrag}/>
<div
        class="draggable"
        class:draggable={!disabled}
        on:mousedown={startDrag}
        role="button"
        tabindex="0"
        style="
        position: absolute;
        left: {position.x}px;
        bottom: {position.y}px;
        {disabled ? 'pointer-events: none;' : ''}
    "
>
    <div class="hud-content-wrapper">
        <slot/>
    </div>
</div>

<style>
    .draggable {
        cursor: move;
        user-select: none;
    }

    .hud-content-wrapper {
        display: inline-block;
        padding: 8px;
    }
</style>
