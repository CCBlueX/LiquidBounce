<script lang="ts">
    import {onMount, tick} from "svelte";
    import {Tween} from 'svelte/motion';
    import {cubicOut, quintOut} from 'svelte/easing';
    import {get, writable} from 'svelte/store';
    import {setContext} from "svelte";
    import type {Module as TModule} from "../../integration/types";
    import type {ModuleToggleEvent} from "../../integration/events";
    import {listen} from "../../integration/ws";
    import {setItem} from "../../integration/persistent_storage";
    import {fly} from 'svelte/transition';
    import Module from "./Module.svelte";
    import {
        gridSize,
        highlightModuleName,
        maxPanelZIndex,
        scaleFactor,
        panelId,
        showGrid,
        snappingEnabled,
        filteredModules,
        effectiveFilteredModules,
        locked,
        savedConfigs,
        panelLength
    } from "./clickgui_store";
    import {debounce} from "lodash";
    import {readable} from 'svelte/store';

    const EDGE_THRESHOLD = 50;
    const UNDO_STACK_LIMIT = 100;
    const ANIMATION_DURATION = 2000;
    const gradientId = `globalGradient-${Math.random().toString(36).slice(2)}`;

    export let modules: TModule[];
    export const showLockHint = writable(false);
    export const saveAnimation = writable<'save' | null>(null);
    export const lockAnimation = writable<'lock' | 'unlock' | null>(null);
    const glowState = writable(false);

    const tween = new Tween(0, {duration: 150, easing: cubicOut});

    const indicatorOpacity = readable(tween.current, (set) => {
        const interval = setInterval(() => {
            set(tween.current);
        }, 16);

        return () => clearInterval(interval);
    });
    export let category: string;

    export let panelIndex: number;


    let panelElement: HTMLElement;
    let modulesElement: HTMLElement;
    let filterMode: 'all' | 'enabled' | 'disabled' = 'all';

    let undoStack: PanelConfig[] = [];
    let lastSaveTime = 0;
    let lastLockToggleTime = 0;
    let moving = false;
    let offsetX = 0;
    let offsetY = 0;
    let scrollPositionSaveTimeout: ReturnType<typeof setTimeout>;
    let ignoreGrid = false;
    let isScrollMode = false;
    let mouseX = 0;
    let hasMoved = false;

    interface PanelConfig {
        top: number;
        left: number;
        expanded: boolean;
        scrollTop: number;
        zIndex: number;
    }

    const storageKey = `clickgui.panel.${panelId}.expandedModule`;
    const expandedModuleName = writable<string | null>(null);
    setContext("expandedModuleName", expandedModuleName);
    const panelConfig = loadPanelConfig();


    $: renderedModules = $effectiveFilteredModules.length > 0
        ? modules.filter(module =>
            $effectiveFilteredModules.some(fm => fm.name === module.name)
        )
        : modules.filter(module => {
            if (!panelConfig.expanded) return false;
            if (filterMode === 'enabled') return module.enabled;
            if (filterMode === 'disabled') return !module.enabled;
            return true;
        });


    function clamp(number: number, min: number, max: number): number {
        return Math.max(min, Math.min(number, max));
    }

    function clonePanelConfig(config: PanelConfig): PanelConfig {
        return JSON.parse(JSON.stringify(config));
    }

    function snapToGrid(value: number): number {
        return (ignoreGrid || !$snappingEnabled)
            ? value
            : Math.round(value / $gridSize) * $gridSize;
    }


    function loadPanelConfig(): PanelConfig {
        const localStorageItem = localStorage.getItem(
            `clickgui.panel.${category}`,
        );

        if (!localStorageItem) {
            return {
                top: panelIndex * 50 + 20,
                left: 20,
                expanded: false,
                scrollTop: 0,
                zIndex: 0
            };
        } else {
            const config: PanelConfig = JSON.parse(localStorageItem);

            // Migration
            if (!config.zIndex) {
                config.zIndex = 0;
            }

            if (config.zIndex > $maxPanelZIndex) {
                $maxPanelZIndex = config.zIndex;
            }


            return config;
        }
    }

    async function savePanelConfig() {
        await setItem(
            `clickgui.panel.${category}`,
            JSON.stringify(panelConfig),
        );
    }

    function fixPosition() {
        panelConfig.left = clamp(
            panelConfig.left,
            0,
            document.documentElement.clientWidth * (2 / $scaleFactor) - panelElement.offsetWidth
        );
        panelConfig.top = clamp(
            panelConfig.top,
            0,
            document.documentElement.clientHeight * (2 / $scaleFactor) - panelElement.offsetHeight
        );
    }

    function applyPanelConfig(config: PanelConfig, saveHistory = false) {
        if (saveHistory) {
            pushUndoState();
        }

        Object.assign(panelConfig, {
            ...config,
            zIndex: config.zIndex === 0 ? 0 : ++$maxPanelZIndex
        });

        fixPosition();
        savePanelConfig();

        tick().then(() => {
            if (modulesElement) {
                modulesElement.scrollTop = panelConfig.scrollTop;
            }
        });
    }


    function pushUndoState() {
        undoStack.push(clonePanelConfig(panelConfig));
        if (undoStack.length > UNDO_STACK_LIMIT) {
            undoStack.shift();
        }
    }


    function onMouseDown(e: MouseEvent) {
        if ($locked) return;

        moving = true;
        hasMoved = false;
        offsetX = e.clientX * (2 / $scaleFactor) - panelConfig.left;
        offsetY = e.clientY * (2 / $scaleFactor) - panelConfig.top;
        panelConfig.zIndex = ++$maxPanelZIndex;
        panelElement.style.transition = "none";
    }

    function onMouseMove(e: MouseEvent) {
        if ($locked || !moving) return;

        if (!hasMoved) {
            hasMoved = true;
            if ($snappingEnabled) $showGrid = true;
        }

        panelConfig.left = snapToGrid(e.clientX * (2 / $scaleFactor) - offsetX);
        panelConfig.top = snapToGrid(e.clientY * (2 / $scaleFactor) - offsetY);
        fixPosition();
        debouncedMouseMove(e);
    }

    function onMouseUp() {
        if (moving) {
            savePanelConfig();
        }
        moving = false;
        $showGrid = false;
        panelElement.style.transition = "all 0.5s ease";
    }

    function toggleExpanded() {
        if ($filteredModules.length > 0) return;

        pushUndoState();
        panelConfig.expanded = !panelConfig.expanded;

        fixPosition();
        savePanelConfig();
    }

    function handleModulesScroll() {
        panelConfig.scrollTop = modulesElement.scrollTop;

        clearTimeout(scrollPositionSaveTimeout);
        scrollPositionSaveTimeout = setTimeout(() => {
            savePanelConfig();
            pushUndoState();
        }, 500);
    }


    const toggleScrollMode = (enabled: boolean) => {
        isScrollMode = enabled;
        tween.target = enabled ? 1 : 0;
    };

    function handleMouseMove(event: MouseEvent) {
        mouseX = event.clientX;
    }

    function handleMouseDown(event: MouseEvent) {
        if (event.button === 1 && mouseX >= window.innerWidth - EDGE_THRESHOLD) {
            event.preventDefault();
            toggleScrollMode(true);
        }
    }

    function handleMouseUp(event: MouseEvent) {
        if (event.button === 2 && isScrollMode) {
            toggleScrollMode(false);
        }
    }

    function handleWheel(event: WheelEvent) {
        if (!isScrollMode) return;

        if (event.deltaY < 0) {
            console.log('Scroll up → previous category');
        } else if (event.deltaY > 0) {
            console.log('Scroll down → next category');
        }
    }


    function handleKeydown(e: KeyboardEvent) {
        if (e.key === "Shift") {
            ignoreGrid = true;
            return;
        }
        if (e.key === " " && !(e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement)) {
            e.preventDefault();
            return;
        }
        if (!e.altKey || e.ctrlKey || e.metaKey) return;

        const key = e.key.toLowerCase();
        if (!['r', 's', 'l', 'd', 'e', 'a', 'z', 'y', 'n'].includes(key)) return;

        e.preventDefault();
        const now = Date.now();

        switch (key) {
            case "l":
                handleLockToggle(now);
                break;
            case "s":
                handleSave(now);
                break;
            case "r":
                handleRestore();
                break;
            case "n":
                handleReset();
                break;
            case "d":
                filterMode = 'disabled';
                break;
            case "e":
                filterMode = 'enabled';
                break;
            case "a":
                filterMode = 'all';
                break;
        }
    }

    function handleLockToggle(now: number) {
        if (now - lastLockToggleTime < ANIMATION_DURATION) return;
        lastLockToggleTime = now;

        locked.update(current => {
            const next = !current;
            lockAnimation.set(next ? 'lock' : 'unlock');
            showLockHint.set(true);

            setTimeout(() => {
                lockAnimation.set(null);
                showLockHint.set(false);
            }, ANIMATION_DURATION);

            return next;
        });
    }

    function handleSave(now: number) {
        if (now - lastSaveTime < ANIMATION_DURATION) return;
        lastSaveTime = now;

        const cfg = clonePanelConfig(panelConfig);
        const all = get(savedConfigs);
        savedConfigs.set({...all, [category]: cfg});

        saveAnimation.set('save');
        setTimeout(() => saveAnimation.set(null), ANIMATION_DURATION);
        glowState.set(true);
        setTimeout(() => glowState.set(false), 1500);
    }

    function handleRestore() {
        const all = get(savedConfigs);
        const cfg = all[category];
        if (cfg) applyPanelConfig(cfg);
    }

    function handleReset() {
        localStorage.removeItem(`clickgui.panel.${category}`);
        const initialConfig = loadPanelConfig();
        applyPanelConfig(initialConfig, true);
    }

    function handleKeyup(e: KeyboardEvent) {
        if (e.key === "Shift") {
            ignoreGrid = false;
        }
    }

    highlightModuleName.subscribe((name) => {
        if (!name || !modulesElement) return;

        requestAnimationFrame(() => {
            const index = modules.findIndex((m) => m.name === name);
            if (index === -1) return;

            panelConfig.zIndex = ++$maxPanelZIndex;
            panelConfig.expanded = true;
            savePanelConfig();

            setTimeout(() => {
                const targetEl = modulesElement.children[index] as HTMLElement;
                if (targetEl) {
                    modulesElement.scrollTo({
                        top: targetEl.offsetTop - 20,
                        behavior: 'smooth'
                    });
                }
            }, 50);
        });
    });
    const debouncedMouseMove = debounce((e: MouseEvent) => {
        if ($locked || !moving) return;

        panelConfig.left = snapToGrid(e.clientX * (2 / $scaleFactor) - offsetX);
        panelConfig.top = snapToGrid(e.clientY * (2 / $scaleFactor) - offsetY);
        fixPosition();
    }, 16);


    listen("moduleToggle", (e: ModuleToggleEvent) => {
        const mod = modules.find((m) => m.name === e.moduleName);
        if (!mod) return;

        pushUndoState();
        mod.enabled = e.enabled;
        modules = modules;
    });
    onMount(() => {
        const last = localStorage.getItem(storageKey);
        if (last) expandedModuleName.set(last);
        if (!modulesElement) {
            return;
        }
        expandedModuleName.subscribe(name => {
            if (name) localStorage.setItem(storageKey, name);
            else localStorage.removeItem(storageKey);
        });
        const options = {passive: true};
        const keydownHandler = (e: KeyboardEvent) => handleKeydown(e);
        const keyupHandler = (e: KeyboardEvent) => handleKeyup(e);
        modulesElement.scrollTo({
            top: panelConfig.scrollTop,
            behavior: "smooth"
        });
        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mousedown', handleMouseDown);
        window.addEventListener('mousemove', debouncedMouseMove, options);
        window.addEventListener('wheel', handleWheel, {passive: false});
        window.addEventListener("keydown", keydownHandler, true);
        window.addEventListener("keyup", keyupHandler, true);

        return () => {
            window.removeEventListener("keydown", keydownHandler, true);
            window.removeEventListener("keyup", keyupHandler, true);
            window.removeEventListener('mousemove', debouncedMouseMove);
            window.removeEventListener('mousedown', handleMouseDown);
            window.removeEventListener('mouseup', handleMouseUp);
            window.removeEventListener('wheel', handleWheel);
        };

    });
</script>

<!-- Scroll Indicator -->
<div class="scroll-indicator" style="opacity: {$indicatorOpacity}"></div>

<!-- Window Event Listeners -->
<svelte:window
        on:keydown={handleKeydown}
        on:keyup={handleKeyup}
        on:mousemove={onMouseMove}
        on:mouseup={onMouseUp}
/>

<!-- Panel Wrapper -->
<div
        bind:this={panelElement}
        class="panel-wrapper {moving ? 'no-transition' : ''}"
        class:expanded={panelConfig.expanded}
        in:fly|global={{y: -100, duration: 200, easing: quintOut}}
        out:fly|global={{y: -100, duration: 200, easing: quintOut}}
        style="left: {panelConfig.left}px; top: {panelConfig.top}px; z-index: {panelConfig.zIndex};
        --panel-height: {panelElement?.offsetHeight || 0}px;"
>
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
            class="panel"
            class:glowing={$glowState}
            class:locked={$locked}

    >
        <!-- Panel Title -->
        <div
                class="title"
                on:contextmenu|preventDefault={toggleExpanded}
                on:mousedown={onMouseDown}
        >
            <img
                    alt="icon"
                    class="icon"
                    src="img/clickgui/icon-{category.toLowerCase()}.svg"
            />
            <span class="category">{category === 'Client' ? 'Client' : category}</span>
            <svg style="display: none;">
                <defs>
                    <linearGradient id="globalGradient" x1="0%" x2="100%" y1="0%" y2="100%">
                        <stop offset="0%" stop-color="var(--gradient-start)"/>
                        <stop offset="100%" stop-color="var(--gradient-end)"/>
                    </linearGradient>
                </defs>
            </svg>
            <!-- Status Indicator -->
            {#if $lockAnimation || $saveAnimation}
                <div class="status-indicator { $locked ? 'locked' : '' }">
                    {#if $lockAnimation}
                        <div class="icon-wrapper
                          { $lockAnimation === 'lock' ? 'lock-animation' : '' }
                          { $lockAnimation === 'unlock' ? 'unlock-animation' : '' }"
                        >
                            {#if $locked}
                                <svg viewBox="0 0 24 24" class="gradient-icon">
                                    <defs>
                                        <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="100%">

                                            <stop offset="0%" stop-color="var(--gradient-start)"/>
                                            <stop offset="100%" stop-color="var(--gradient-end)"/>
                                        </linearGradient>
                                    </defs>

                                    <path
                                            fill={"url(#" + gradientId + ")"}
                                            d="M12 3a4 4 0 0 1 4 4v3h1a2 2 0 0 1 2
                                             2v8a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-8a2 2
                                             0 0 1 2-2h1V7a4 4 0 0 1 4-4m0 2a2 2 0 0
                                              0-2 2v3h4V7a2 2 0 0 0-2-2Z"
                                    />
                                </svg>

                            {:else}
                                <svg viewBox="0 0 24 24" class="gradient-icon">
                                    <defs>
                                        <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="100%">
                                            <stop offset="0%" stop-color="var(--gradient-start)"/>
                                            <stop offset="100%" stop-color="var(--gradient-end)"/>
                                        </linearGradient>
                                    </defs>
                                    <path
                                            fill={"url(#" + gradientId + ")"}
                                            d="M18 8h-1V7a5 5 0 0 0-9.9-1.2l2 1.5A3 3 0 0 1 15
                                            7v1H7a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2v
                                            -8a2 2 0 0 0-2-2Z"/>
                                </svg>
                            {/if}
                        </div>
                    {/if}
                </div>
            {/if}

            <!-- Expand Toggle Button -->
            <!-- svelte-ignore a11y_consider_explicit_label -->
            <button class="expand-toggle" on:click={toggleExpanded}>
                <div class="icon" class:expanded={panelConfig.expanded || $filteredModules.length > 0}></div>
            </button>
        </div>

        <!-- Modules List -->
        <div
                class="modules"
                on:scroll={handleModulesScroll}
                bind:this={modulesElement}
                style="--duration: 0.3s; {panelConfig.expanded ? `max-height: ${2 / $scaleFactor * $panelLength}vh` : ''}"
        >
            {#each renderedModules as {name, enabled, description, aliases} (name)}
                <div>
                    <Module {name} {enabled} {description} {aliases}/>
                </div>
            {/each}
        </div>
    </div>
</div>
<style lang="scss">
  @use "../../colors" as *;

  .gradient-icon {
    --gradient-start: var(--primary-color);
    --gradient-end: var(--secondary-color);
  }

  .panel-wrapper {
    position: absolute;
    border-radius: 12px;
    padding: 4px;
    background: transparent;
    box-shadow: 0 0 10px rgba($base, 0.4);
    transition: all 0.5s ease;

    &.no-transition {
      transition: none;
    }

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: inherit;
      padding: 2px;

      background: linear-gradient(
                      120deg,
                      color-mix(in srgb, var(--primary-color) 20%, transparent) 25%,
                      color-mix(in srgb, var(--secondary-color) 20%, transparent) 50%,
                      color-mix(in srgb, var(--primary-color) 20%, transparent) 75%
      );
      background-size: 200% 200%;

      pointer-events: none;
      z-index: 0;
      opacity: 1;
    }
  }

  .scroll-indicator {
    position: fixed;
    top: 50%;
    right: 0;
    transform: translateY(-50%);
    width: 4px;
    height: 100px;
    background: white;
    opacity: 0;
    pointer-events: none;
  }

  .panel {
    width: 230px;
    max-width: 100%;
    position: relative;
    overflow: hidden;
    will-change: transform;
    background-color: rgba($base, 0.5);
    border-radius: 8px;
    height: 100%;
    box-sizing: border-box;
    box-shadow: 0 0 8px color-mix(in srgb, var(--primary-color) 60%, transparent);
    display: flex;
    flex-direction: column;
    transition: transform 0.3s ease;


    --glow-color-1: var(--primary-color);
    --glow-color-2: color-mix(in srgb, var(--primary-color) 85%, white 15%);
    --glow-opacity: 0.5;
  }

  .panel.glowing {
    isolation: isolate;

    &::before {
      content: '';
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      border-radius: 10px;
      background: linear-gradient(
                      135deg,
                      color-mix(in srgb, var(--glow-color-1) 50%, transparent),
                      color-mix(in srgb, var(--glow-color-2) 50%, transparent),
                      color-mix(in srgb, var(--glow-color-1) 50%, transparent)
      );

      background-size: 300% 300%;
      z-index: -1;
      opacity: 0;
      animation: glowGradient 1.5s ease-out,
      glowMovement 3s linear infinite;
    }
  }


  .status-indicator {
    display: flex;
    align-items: center;
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    font-size: var(--font-size);
    color: $text-color;
    border-radius: 4px;
    transition: all 0.3s ease;
    animation: fadeInOut 2s ease;


    &.locked::before {
      opacity: 1;
      animation: pulse 1.5s infinite;
    }

    .icon-wrapper {
      width: 64px;
      height: 64px;
    }

    .lock-animation {
      animation: lockEffect 2s ease-out;
    }

    .unlock-animation {
      animation: unlockEffect 2s ease-out;
    }

    svg {
      width: 64px;
      height: 64px;
    }
  }

  .panel-wrapper:hover:not(.expanded) {
    box-shadow: 0 0 20px color-mix(in srgb, var(--primary-color) 60%, transparent);
    transform: translateY(-4px);
  }

  .title {
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    align-items: center;
    column-gap: 12px;
    cursor: grab;
    text-align: center;
    text-shadow: 0 0 10px color-mix(in srgb, var(--primary-color) 30%, transparent);
    border-radius: 8px 8px 0 0;
    transition: all 0.3s ease;
    padding: 10px 15px;

    .panel:not(.expanded) & {
      background: rgba($mantle, 0.6);
      box-shadow: inset 0 0 10px color-mix(in srgb, var(--primary-color) 20%, transparent);
      text-shadow: 0 0 5px color-mix(in srgb, var(--primary-color) 20%, transparent);
    }

    .icon {
      width: 20px;
      height: 20px;
      object-fit: contain;
      display: block;
    }

    .category {
      font-size: calc(var(--font-size) + 2px);
      color: $text-color;
      font-weight: 600;
      transition: color 0.3s ease;
    }
  }

  .modules {
    overflow-y: auto;
    overflow-x: hidden;
    transition: max-height 0.2s ease;
    scroll-behavior: smooth;
    max-height: 0;
  }

  .modules::-webkit-scrollbar {
    width: 0;
  }

  .expand-toggle {
    background-color: transparent;
    border: none;
    cursor: pointer;

    .icon {
      position: relative;
      height: 12px;
      width: 12px;
      z-index: 1;

      &::before {
        content: "";
        position: absolute;
        background-color: white;
        transition: transform 0.4s ease-out;
        top: 0;
        left: 50%;
        width: 2px;
        height: 100%;
        margin-left: -1px;
      }

      &::after {
        content: "";
        position: absolute;
        background-color: white;
        transition: transform 0.4s ease-out;
        top: 50%;
        left: 0;
        width: 100%;
        height: 2px;
        margin-top: -1px;
      }

      &.expanded {
        &::before {
          transform: rotate(90deg);
        }

        &::after {
          transform: rotate(180deg);
        }
      }
    }
  }

  @keyframes lockEffect {
    0% {
      opacity: 0;
      transform: translateY(20px) scale(0.8);
    }
    40% {
      opacity: 1;
      transform: translateY(0) scale(1.1);
    }
    60% {
      transform: scale(1);
    }
    100% {
      opacity: 0;
      transform: scale(1.2);
    }
  }

  @keyframes unlockEffect {
    0% {
      opacity: 1;
      transform: scale(1) rotate(0deg);
    }
    60% {
      transform: scale(1.2) rotate(-30deg);
    }
    100% {
      opacity: 0;
      transform: scale(0.5) rotate(45deg);
    }
  }

  @keyframes pulse {
    0% {
      box-shadow: 0 0 0 0 color-mix(in srgb, var(--primary-color) 40%, transparent);
    }
    70% {
      box-shadow: 0 0 0 10px color-mix(in srgb, var(--primary-color) 0%, transparent);
    }
    100% {
      box-shadow: 0 0 0 0 color-mix(in srgb, var(--primary-color) 0%, transparent);
    }
  }

  @keyframes fadeInOut {
    0% {
      opacity: 0;
    }
    20% {
      opacity: 1;
    }
    80% {
      opacity: 1;
    }
    100% {
      opacity: 0;
    }
  }

  @keyframes gentlePulse {
    0% {
      opacity: 0;
      transform: scale(0.95);
    }
    50% {
      opacity: 1;
      transform: scale(1.02);
    }
    100% {
      opacity: 0;
      transform: scale(0.95);
    }
  }

  @keyframes saveEnter {
    0% {
      transform: translateY(-100%);
      opacity: 0;
    }
    100% {
      transform: translateY(0);
      opacity: 1;
    }
  }

  @keyframes glowGradient {
    0%, 100% {
      opacity: 0;
    }
    50% {
      opacity: 1;
    }
  }

  @keyframes glowMovement {
    0% {
      background-position: 0 50%;
    }
    100% {
      background-position: 100% 50%;
    }
  }
</style>
