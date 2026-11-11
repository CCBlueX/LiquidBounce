<script lang="ts">
    import type {CurveSetting, ModuleSetting, Vec2} from "../../../integration/types";
    import SettingRow from "./common/SettingRow.svelte";
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import {
        Chart,
        type Chart as ChartJS,
        LinearScale,
        LineController,
        LineElement,
        PointElement,
        ScatterController
    } from "chart.js";
    import dragDataPlugin from "chartjs-plugin-dragdata";

    export let setting: ModuleSetting;

    const cSetting = setting as CurveSetting;

    const dispatch = createEventDispatcher();

    type TChart = ChartJS<'line', Vec2[], unknown>;

    let canvasElement: HTMLCanvasElement;
    let chart: TChart | null = null;

    Chart.register(LinearScale, PointElement, LineElement, LineController, ScatterController, dragDataPlugin);

    let isDragging = false;
    const EPS = 1e-9;
    // Points at the exact edges of the x-axis are locked. This margin prevents additional points from being locked.
    const EDGE_MARGIN = 1e-6;
    const TICK_FONT = {family: "Inter", size: 10};
    let themeObserver: MutationObserver | null = null;

    function getThemeColor(name: string) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    function applyThemeColors() {
        if (!chart) {
            return;
        }

        const accentColor = getThemeColor("--clickgui-curve-accent-color");
        const gridColor = getThemeColor("--clickgui-curve-grid-color");
        const axisColor = getThemeColor("--clickgui-curve-axis-color");

        const dataset = chart.data.datasets[0];
        dataset.borderColor = accentColor;
        dataset.pointBackgroundColor = accentColor;
        dataset.pointHoverBackgroundColor = accentColor;

        const xAxis = chart.options.scales?.x as any;
        const yAxis = chart.options.scales?.y as any;

        xAxis.grid.color = gridColor;
        yAxis.grid.color = gridColor;
        xAxis.ticks.color = axisColor;
        yAxis.ticks.color = axisColor;

        chart.update();
    }

    function clamp(v: number, min: number, max: number) {
        return Math.min(Math.max(v, min), max)
    }

    function sortPoints(arr: Vec2[]) {
        return arr.sort((a, b) => a.x - b.x);
    }

    function updateValue() {
        if (!chart) return;
        cSetting.value = chart.data.datasets[0].data.map(point => ({x: point.x, y: point.y}));
        setting = { ...cSetting };
        dispatch("change");
    }

    /**
     * Ensures that there is always one point at the exact edges of the x-axis.
     */
    function ensureEndpoints() {
        if (!chart) return;

        const dataset = chart.data.datasets[0];

        const findAtX = (x: number) => dataset.data.find((p: Vec2) => Math.abs(p.x - x) <= EPS);

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

    function lockEdgePoints(previousPoint: Vec2, currentPoint: Vec2) {
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
                    borderColor: getThemeColor("--clickgui-curve-accent-color"),
                    pointRadius: 5,
                    pointBackgroundColor: getThemeColor("--clickgui-curve-accent-color"),
                    pointBorderWidth: 0,
                    pointHoverRadius: 6,
                    pointHoverBackgroundColor: getThemeColor("--clickgui-curve-accent-color"),
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
                            color: getThemeColor("--clickgui-curve-grid-color")
                        },
                        ticks: {
                            color: getThemeColor("--clickgui-curve-axis-color"),
                            font: TICK_FONT,
                            maxTicksLimit: 5
                        }
                    },
                    y: {
                        type: "linear",
                        min: cSetting.yAxis.range.from,
                        max: cSetting.yAxis.range.to,
                        grid: {
                            color: getThemeColor("--clickgui-curve-grid-color")
                        },
                        ticks: {
                            color: getThemeColor("--clickgui-curve-axis-color"),
                            font: TICK_FONT,
                            maxTicksLimit: 5
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
                            const currentPoint = value as Vec2;

                            lockEdgePoints(previousPoint, currentPoint);
                        },
                        onDragEnd: (_e, datasetIndex, index, value) => {
                            if (!chart) return;

                            const dataset = chart.data.datasets[datasetIndex];
                            const previousPoint = dataset.data[index];
                            const currentPoint = value as Vec2;

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
        applyThemeColors();
        chart.update();

        themeObserver = new MutationObserver(() => {
            applyThemeColors();
        });
        themeObserver.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ["style", "class"]
        });
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
        themeObserver?.disconnect();
        themeObserver = null;
        chart?.destroy();
        chart = null;
    });
</script>

<SettingRow {setting}>
    <span class="axes">{cSetting.yAxis.label} over {cSetting.xAxis.label}</span>

    <div slot="below" class="canvas-wrapper">
        <canvas on:click={addPoint} on:contextmenu={removePoint}
                bind:this={canvasElement}></canvas>
    </div>
</SettingRow>

<style lang="scss">
  .axes {
    font-size: 11px;
    color: var(--clickgui-text-muted-color);
    text-align: right;
  }

  .canvas-wrapper {
    position: relative;
    height: 140px;
    margin-bottom: 8px;
  }
</style>
