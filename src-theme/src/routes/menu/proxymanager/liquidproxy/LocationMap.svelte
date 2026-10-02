<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import {geoMercator, geoPath} from "d3-geo";
    import {feature} from "topojson-client";
    import type {Feature, FeatureCollection} from "geojson";
    import worldUrl from "world-atlas/countries-50m.json?url";
    import type {LiquidProxyLocation} from "../../../../integration/types";
    import ToolTip from "../../common/ToolTip.svelte";

    export let locations: LiquidProxyLocation[] = [];
    export let connected: string | undefined = undefined;
    // Where the map opens when nothing is connected, e.g. the location chosen last
    export let focus: string | undefined = undefined;
    export let interactive = true;

    const dispatch = createEventDispatcher<{ connect: string, disconnect: void }>();

    const MIN_ZOOM = 1;
    const MAX_ZOOM = 24;
    const START_ZOOM = 4.5;
    const ZOOM_STEP = 1.5;
    // Central Europe, where most locations are
    const DEFAULT_CENTER: [number, number] = [12, 50];

    let width = 0;
    let height = 0;
    let countries: Feature[] = [];

    let zoom = START_ZOOM;
    let x = 0;
    let y = 0;
    // Until the user pans or zooms, the map follows its size and the location it should show
    let moved = false;

    let drag: { pointerX: number, pointerY: number, x: number, y: number } | null = null;

    $: projection = geoMercator().scale(width / (2 * Math.PI)).translate([width / 2, height / 2]);
    $: paths = countries.map(country => geoPath(projection)(country) ?? "");
    $: connectedLocation = locations.find(l => l.code === connected);

    $: if (!moved && width > 0 && height > 0) {
        const target = locations.find(l => l.code === (connected ?? focus));
        center(target?.longitude !== undefined && target.latitude !== undefined
            ? [target.longitude, target.latitude]
            : DEFAULT_CENTER);
    }

    onMount(async () => {
        const topology = await (await fetch(worldUrl)).json();
        countries = (feature(topology, topology.objects.countries) as unknown as FeatureCollection).features;
    });

    // Screen position of each location with coordinates
    $: positions = new Map(locations.flatMap(location => {
        if (location.longitude === undefined || location.latitude === undefined) {
            return [];
        }
        const [px, py] = projection([location.longitude, location.latitude]) ?? [0, 0];
        return [[location.code, [x + zoom * px, y + zoom * py] as [number, number]]];
    }));

    function center(coordinates: [number, number]) {
        const [px, py] = projection(coordinates) ?? [0, 0];
        x = width / 2 - zoom * px;
        y = height / 2 - zoom * py;
    }

    function zoomAt(factor: number, cx: number, cy: number) {
        moved = true;
        const next = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, zoom * factor));
        x = cx - (cx - x) * next / zoom;
        y = cy - (cy - y) * next / zoom;
        zoom = next;
    }

    function handleWheel(e: WheelEvent) {
        const bounds = (e.currentTarget as HTMLElement).getBoundingClientRect();
        zoomAt(e.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP, e.clientX - bounds.left, e.clientY - bounds.top);
    }

    function handlePointerDown(e: PointerEvent) {
        moved = true;
        drag = {pointerX: e.clientX, pointerY: e.clientY, x, y};
        (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    }

    function handlePointerMove(e: PointerEvent) {
        if (drag) {
            x = drag.x + e.clientX - drag.pointerX;
            y = drag.y + e.clientY - drag.pointerY;
        }
    }

    function isAvailable(location: LiquidProxyLocation) {
        return !location.maintenance && (!location.probed || location.latency !== undefined);
    }

    // Reactive, so that it follows the connection
    $: tooltip = (location: LiquidProxyLocation) => {
        if (location.maintenance) {
            return `${location.label} · Under maintenance`;
        }
        if (location.probed && location.latency === undefined) {
            return `${location.label} · Not reachable`;
        }

        let text = location.label;
        if (interactive) {
            text = location.code === connected ? `Disconnect from ${text}` : `Connect to ${text}`;
        }
        return location.latency !== undefined ? `${text} · ${location.latency} ms` : text;
    };

    function handleLocationClick(location: LiquidProxyLocation) {
        if (!interactive || !isAvailable(location)) {
            return;
        }
        if (location.code === connected) {
            dispatch("disconnect");
        } else {
            dispatch("connect", location.code);
        }
    }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="map" class:dragging={drag !== null} class:idle={!interactive}
     bind:clientWidth={width} bind:clientHeight={height}
     on:wheel|preventDefault={handleWheel}
     on:pointerdown={handlePointerDown}
     on:pointermove={handlePointerMove}
     on:pointerup={() => drag = null}>
    <svg {width} {height}>
        <g transform="translate({x} {y}) scale({zoom})">
            {#each paths as d}
                <path {d} vector-effect="non-scaling-stroke"/>
            {/each}
        </g>
    </svg>

    {#each locations as location, index (location.code)}
        {@const position = positions.get(location.code)}
        {#if position}
            <button class="location" aria-label={location.label}
                    class:connected={location.code === connected}
                    class:unavailable={!isAvailable(location)}
                    style="left: {position[0]}px; top: {position[1]}px; --pulse-delay: {index * 0.35}s;"
                    on:pointerdown|stopPropagation={() => moved = true}
                    on:click={() => handleLocationClick(location)}>
                <ToolTip text={tooltip(location)}
                         color={isAvailable(location) ? "var(--tooltip-background-color)" : "var(--menu-base-68-color)"}/>
                <span class="dot"></span>
            </button>
        {/if}
    {/each}

    <!-- A connection stays visible, and can be ended, even when nothing else here can be used -->
    {#if interactive || connectedLocation}
        <div class="status">
            {#if connectedLocation}
                <button class="pill connected" on:pointerdown|stopPropagation on:click={() => dispatch("disconnect")}>
                    Connected to {connectedLocation.label}
                    <img src="img/menu/icon-disconnect.svg" alt="disconnect">
                </button>
            {:else}
                <span class="pill disconnected">Not connected</span>
            {/if}
        </div>
    {/if}

    <div class="zoom" on:pointerdown|stopPropagation>
        <button on:click={() => zoomAt(1 / ZOOM_STEP, width / 2, height / 2)}>−</button>
        <button on:click={() => zoomAt(ZOOM_STEP, width / 2, height / 2)}>+</button>
    </div>
</div>

<style lang="scss">
  .map {
    position: relative;
    overflow: hidden;
    border-radius: 5px;
    background-color: rgba(0, 0, 0, 0.25);
    cursor: grab;
    user-select: none;
    min-height: 0;

    &.dragging {
      cursor: grabbing;
    }
  }

  svg {
    position: absolute;
    inset: 0;

    path {
      fill: rgba(255, 255, 255, 0.06);
      stroke: rgba(255, 255, 255, 0.14);
      stroke-width: 1;
    }
  }

  .location {
    position: absolute;
    transform: translate(-50%, -50%);
    background: none;
    border: none;
    padding: 6px;
    cursor: pointer;

    .dot {
      display: block;
      width: 18px;
      height: 18px;
      border-radius: 50%;
      background-color: color-mix(in srgb, var(--accent-color) 65%, transparent);
      box-shadow: inset 0 0 0 5px var(--accent-color);
      transition: ease transform .2s;
    }

    &:hover {
      z-index: 2;

      .dot {
        transform: scale(1.3);
      }
    }

    &.connected .dot {
      background-color: color-mix(in srgb, var(--success-color) 65%, transparent);
      box-shadow: inset 0 0 0 5px var(--success-color);
    }

    &.unavailable {
      cursor: default;

      .dot {
        background-color: rgba(255, 255, 255, 0.15);
        box-shadow: inset 0 0 0 5px rgba(255, 255, 255, 0.3);
      }
    }
  }

  .idle .location:not(.unavailable):not(.connected) .dot {
    animation: pulse 2.8s ease-out infinite;
    animation-delay: var(--pulse-delay);
  }

  @keyframes pulse {
    0% {
      box-shadow: inset 0 0 0 5px var(--accent-color), 0 0 0 0 color-mix(in srgb, var(--accent-color) 55%, transparent);
    }
    70%, 100% {
      box-shadow: inset 0 0 0 5px var(--accent-color), 0 0 0 14px transparent;
    }
  }

  .status {
    position: absolute;
    top: 15px;
    right: 15px;
  }

  .pill {
    display: flex;
    align-items: center;
    column-gap: 6px;
    border: none;
    color: var(--menu-text-color);
    font-family: inherit;
    font-size: 13px;
    font-weight: 500;
    padding: 5px 12px;
    border-radius: 20px;

    img {
      height: 12px;
    }

    &.connected {
      background-color: var(--success-color);
      cursor: pointer;
    }

    &.disconnected {
      background-color: var(--error-color);
    }
  }

  .zoom {
    position: absolute;
    right: 15px;
    bottom: 15px;
    display: flex;
    border-radius: 20px;
    overflow: hidden;
    background-color: rgba(0, 0, 0, 0.6);

    button {
      width: 28px;
      height: 24px;
      border: none;
      background: none;
      color: var(--menu-text-color);
      font-family: inherit;
      font-size: 15px;
      cursor: pointer;

      &:hover {
        background-color: var(--accent-color);
      }
    }
  }
</style>
