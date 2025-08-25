<script lang="ts">
    import type {CurveSetting, ModuleSetting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {onMount, onDestroy} from "svelte";
    import {
        Chart,
        LinearScale,
        PointElement,
        LineElement,
        ScatterController,
        type ScatterDataPoint,
        type Chart as ChartJS, type Point, LineController
    } from "chart.js";
    import dragDataPlugin from "chartjs-plugin-dragdata";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import {setItem} from "../../../integration/persistent_storage";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as CurveSetting;

    const thisPath = `${path}.${cSetting.name}`;
    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

    type TChart = ChartJS<'line', ScatterDataPoint[], unknown>;

    let canvasElement: HTMLCanvasElement;
    let chart: TChart | null = null;

    Chart.register(LinearScale, PointElement, LineElement, LineController, ScatterController, dragDataPlugin);

    let isDragging = false;
    const EPS = 1e-9;
    // Points at the exact edges of the x-axis are locked. This margin prevents additional points from being locked.
    const EDGE_MARGIN = 1e-6;
    const COLOR_ACCENT = "#4677ff"; // NOTE: This should be read from a color file in the future.
    const COLOR_GRID = "#333333";
    const COLOR_DIMMED_TEXT = "rgba(211, 211, 211, 255)";

    function clamp(v: number, min: number, max: number) {
        return Math.min(Math.max(v, min), max)
    }

    function sortPoints(arr: ScatterDataPoint[]) {
        return arr.sort((a, b) => a.x - b.x);
    }

    function updateValue() {
        if (!chart) return;
        const ds = chart.data.datasets[0] as any;
        const tuples = ds.data.map((p: ScatterDataPoint) => [p.x, p.y]) as [number, number][];
        console.log(tuples);
    }

    /**
     * Ensures that there is always one point at the exact edges of the x-axis.
     */
    function ensureEndpoints() {
        if (!chart) return;

        const dataset = chart.data.datasets[0];

        const findAtX = (x: number) => dataset.data.find((p: ScatterDataPoint) => Math.abs(p.x - x) <= EPS);

        if (findAtX(cSetting.minX) === undefined) {
            dataset.data.push({x: cSetting.minX, y: cSetting.minY / 2});
        }
        if (findAtX(cSetting.maxX) === undefined) {
            dataset.data.push({x: cSetting.maxX, y: cSetting.minY / 2});
        }

        for (let p of dataset.data) {
            if (Math.abs(p.x - cSetting.minX) <= EPS) {
                p.x = cSetting.minX;
            }
            if (Math.abs(p.x - cSetting.maxX) <= EPS) {
                p.x = cSetting.maxX;
            }
        }

        sortPoints(dataset.data);
    }

    /**
     * Finds the clicked x and y position within the chart's canvas.
     * @param e The mouse event to find the position of.
     * @param c The chart to find the position within.
     */
    function getPositionInChart(e: MouseEvent, c: TChart) {
        const rect = (c.canvas as HTMLCanvasElement).getBoundingClientRect();
        const xPixel = e.clientX - rect.left;
        const yPixel = e.clientY - rect.top;
        const xs = c.scales.x as any;
        const ys = c.scales.y as any;
        return {
            xPixel, yPixel,
            x: xs.getValueForPixel(xPixel),
            y: ys.getValueForPixel(yPixel)
        };
    }

    function lockEdgePoints(previousPoint: Point, currentPoint: Point) {
        const minOpen = cSetting.minX + EDGE_MARGIN;
        const maxOpen = cSetting.maxX - EDGE_MARGIN;

        // Determine endpoint by X position (non-endpoints can never equal minX/maxX due to open-interval clamp)
        const isMinEndpoint = Math.abs(previousPoint.x - cSetting.minX) <= EPS;
        const isMaxEndpoint = Math.abs(previousPoint.x - cSetting.maxX) <= EPS;

        if (isMinEndpoint) {
            currentPoint.x = cSetting.minX; // lock X
        } else if (isMaxEndpoint) {
            currentPoint.x = cSetting.maxX; // lock X
        } else {
            currentPoint.x = clamp(currentPoint.x, minOpen, maxOpen); // keep away from exact edges
        }

        currentPoint.y = clamp(currentPoint.y, cSetting.minY, cSetting.maxY);
    }

    onMount(() => {
        const ctx = canvasElement.getContext("2d")!;

        chart = new Chart(ctx, {
            type: "line",
            data: {
                datasets: [{
                    type: "line",
                    data: sortPoints(cSetting.value.map(([x, y]) => ({x, y}))),
                    showLine: true,
                    parsing: false,
                    borderWidth: 2,
                    borderColor: COLOR_ACCENT,
                    pointRadius: 5,
                    pointBackgroundColor: COLOR_ACCENT,
                    pointBorderWidth: 0,
                    pointHoverRadius: 6,
                    pointHoverBackgroundColor: COLOR_ACCENT,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: {
                        type: "linear",
                        min: cSetting.minX,
                        max: cSetting.maxX,
                        grid: {
                            color: COLOR_GRID
                        },
                        ticks: {
                            color: COLOR_DIMMED_TEXT
                        },
                        title: {
                            display: true,
                            text: "X Axis",
                            color: COLOR_DIMMED_TEXT
                        }
                    },
                    y: {
                        type: "linear",
                        min: cSetting.minY,
                        max: cSetting.maxY,
                        grid: {
                            color: COLOR_GRID
                        },
                        ticks: {
                            color: COLOR_DIMMED_TEXT
                        },
                        title: {
                            display: true,
                            text: "Y Axis",
                            color: COLOR_DIMMED_TEXT
                        }
                    }
                },
                plugins: {
                    legend: {display: false},
                    tooltip: {enabled: false},
                    dragData: {
                        dragX: true,
                        onDragStart: () => {
                            isDragging = true;
                        },
                        onDrag: (_e, datasetIndex, index, value) => {
                            if (!chart) return;

                            const previousPoint = chart.data.datasets[datasetIndex].data[index];
                            const currentPoint = value as Point;

                            lockEdgePoints(previousPoint, currentPoint);
                        },
                        onDragEnd: (_e, datasetIndex, index, value) => {
                            if (!chart) return;

                            const dataset = chart.data.datasets[datasetIndex];
                            const previousPoint = dataset.data[index];
                            const currentPoint = value as Point;

                            lockEdgePoints(previousPoint, currentPoint);
                            sortPoints(dataset.data);

                            chart.update();

                            isDragging = false;
                            ensureEndpoints(); // ensure end points still exist and snap to exact min/max
                            chart.update();
                            updateValue();
                        }
                    }
                }
            }
        });

        // Ensure endpoints exist and snap exactly to min/max at startup
        ensureEndpoints();
        chart.update();
    });

    // Adds a new point close to the position that was clicked.
    function addPoint(e: MouseEvent) {
        if (!chart || isDragging) return;

        const {x, y} = getPositionInChart(e, chart);
        const minOpen = cSetting.minX + EDGE_MARGIN;
        const maxOpen = cSetting.maxX - EDGE_MARGIN;

        const nx = clamp(x, minOpen, maxOpen);
        const ny = clamp(y, cSetting.minY, cSetting.maxY);

        const dataset = chart.data.datasets[0];
        dataset.data.push({x: nx, y: ny});
        sortPoints(dataset.data);
        ensureEndpoints();
        chart.update();
        updateValue();
    }

    // Removes a point which was right clicked
    function removePoint(e: MouseEvent) {
        e.preventDefault();
        if (!chart) return;

        const hits = chart.getElementsAtEventForMode(e, "nearest", {intersect: true}, true);
        if (!hits.length) return;

        const {datasetIndex, index} = hits[0];
        const dataset = chart.data.datasets[datasetIndex];
        const p = dataset.data[index];

        // Don't remove the required endpoints
        if (Math.abs(p.x - cSetting.minX) <= EPS) return;
        if (Math.abs(p.x - cSetting.maxX) <= EPS) return;

        dataset.data.splice(index, 1);
        sortPoints(dataset.data);
        ensureEndpoints();
        chart.update();
        updateValue();
    }

    onDestroy(() => {
        chart?.destroy();
        chart = null;
    });
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" class:expanded on:contextmenu|preventDefault={() => expanded = !expanded}>
        <div class="title">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <ExpandArrow bind:expanded/>
    </div>

    <div class="canvas-wrapper" class:visible={expanded}>
        <canvas on:click={addPoint} on:contextmenu={removePoint}
                bind:this={canvasElement}></canvas>
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
    position: relative;
  }

  .canvas-wrapper {
    height: 0;
    opacity: 0;
    overflow: hidden;
    will-change: height, opacity;
    transition: ease height 0.2s, ease opacity 0.2s;

    &.visible {
      height: 180px;
      opacity: 1;
    }
  }

  .title {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 600;
  }

  .head {
    display: flex;
    justify-content: space-between;
    transition: ease margin-bottom .2s;

    &.expanded {
      margin-bottom: 10px;
    }
  }

</style>
