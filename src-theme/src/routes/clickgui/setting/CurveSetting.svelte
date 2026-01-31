<script lang="ts">
    import type {CurveSetting, ModuleSetting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import {
        Chart,
        type Chart as ChartJS,
        LinearScale,
        LineController,
        LineElement,
        type Point,
        PointElement,
        ScatterController,
        type ScatterDataPoint
    } from "chart.js";
    import dragDataPlugin from "chartjs-plugin-dragdata";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import {setItem} from "../../../integration/persistent_storage";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as CurveSetting;

    const dispatch = createEventDispatcher();

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
        cSetting.value = ds.data.map((p: ScatterDataPoint) => ({x: p.x, y: p.y})) as Point[];
        setting = { ...cSetting };
        dispatch("change");
    }

    /**
     * Ensures that there is always one point at the exact edges of the x-axis.
     */
    function ensureEndpoints() {
        if (!chart) return;

        const dataset = chart.data.datasets[0];

        const findAtX = (x: number) => dataset.data.find((p: ScatterDataPoint) => Math.abs(p.x - x) <= EPS);

        if (findAtX(cSetting.xAxis.range.from) === undefined) {
            dataset.data.push({x: cSetting.xAxis.range.from, y: cSetting.yAxis.range.from / 2});
        }
        if (findAtX(cSetting.xAxis.range.to) === undefined) {
            dataset.data.push({x: cSetting.xAxis.range.to, y: cSetting.yAxis.range.from / 2});
        }

        for (let p of dataset.data) {
            if (Math.abs(p.x - cSetting.xAxis.range.from) <= EPS) {
                p.x = cSetting.xAxis.range.from;
            }
            if (Math.abs(p.x - cSetting.xAxis.range.to) <= EPS) {
                p.x = cSetting.xAxis.range.to;
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
        const minOpen = cSetting.xAxis.range.from + EDGE_MARGIN;
        const maxOpen = cSetting.xAxis.range.to - EDGE_MARGIN;

        // Determine endpoint by X position (non-endpoints can never equal xAxis.range.from/xAxis.range.to due to open-interval clamp)
        const isMinEndpoint = Math.abs(previousPoint.x - cSetting.xAxis.range.from) <= EPS;
        const isMaxEndpoint = Math.abs(previousPoint.x - cSetting.xAxis.range.to) <= EPS;

        if (isMinEndpoint) {
            currentPoint.x = cSetting.xAxis.range.from; // lock X
        } else if (isMaxEndpoint) {
            currentPoint.x = cSetting.xAxis.range.to; // lock X
        } else {
            currentPoint.x = clamp(currentPoint.x, minOpen, maxOpen); // keep away from exact edges
        }

        currentPoint.y = clamp(currentPoint.y, cSetting.yAxis.range.from, cSetting.yAxis.range.to);
    }

    onMount(() => {
        const ctx = canvasElement.getContext("2d")!;

        chart = new Chart(ctx, {
            type: "line",
            data: {
                datasets: [{
                    type: "line",
                    data: sortPoints(cSetting.value.map(point => ({x: point.x, y: point.y}))),
                    showLine: true,
                    parsing: false,
                    borderWidth: 2,
                    borderColor: COLOR_ACCENT,
                    pointRadius: 5,
                    pointBackgroundColor: COLOR_ACCENT,
                    pointBorderWidth: 0,
                    pointHoverRadius: 6,
                    pointHoverBackgroundColor: COLOR_ACCENT,
                    tension: cSetting.tension
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: {
                        type: "linear",
                        min: cSetting.xAxis.range.from,
                        max: cSetting.xAxis.range.to,
                        grid: {
                            color: COLOR_GRID
                        },
                        ticks: {
                            color: COLOR_DIMMED_TEXT
                        },
                        title: {
                            display: true,
                            text: cSetting.xAxis.label,
                            color: COLOR_DIMMED_TEXT
                        }
                    },
                    y: {
                        type: "linear",
                        min: cSetting.yAxis.range.from,
                        max: cSetting.yAxis.range.to,
                        grid: {
                            color: COLOR_GRID
                        },
                        ticks: {
                            color: COLOR_DIMMED_TEXT
                        },
                        title: {
                            display: true,
                            text: cSetting.yAxis.label,
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
        const minOpen = cSetting.xAxis.range.from + EDGE_MARGIN;
        const maxOpen = cSetting.xAxis.range.to - EDGE_MARGIN;

        const nx = clamp(x, minOpen, maxOpen);
        const ny = clamp(y, cSetting.yAxis.range.from, cSetting.yAxis.range.to);

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
        if (Math.abs(p.x - cSetting.xAxis.range.from) <= EPS) return;
        if (Math.abs(p.x - cSetting.xAxis.range.to) <= EPS) return;

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
