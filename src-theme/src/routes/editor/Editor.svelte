<script lang="ts">
    import { onMount } from "svelte";
    import {
        createComponent,
        deleteComponent,
        getAllComponents,
        getComponentFactories,
        getGameWindow
    } from "../../integration/rest";
    import { listen } from "../../integration/ws";
    import type { Component, ComponentFactories } from "../../integration/types";
    import type { ScaleFactorChangeEvent } from "../../integration/events";

    let zoom = 100;
    let components: Component[] = [];
    let componentFactories: ComponentFactories[] = [];
    let startX = 0;
    let startY = 0;
    let isDragging = false;
    let panelX = 250;
    let panelY = 250;

    onMount(async () => {
        const gameWindow = await getGameWindow();
        zoom = gameWindow.scaleFactor * 50;

        components = await getAllComponents();
        componentFactories = await getComponentFactories();
    });

    listen("scaleFactorChange", (data: ScaleFactorChangeEvent) => {
        zoom = data.scaleFactor * 50;
    });

    const startDrag = (event: MouseEvent) => {
        isDragging = true;
        startX = event.clientX - panelX;
        startY = event.clientY - panelY;
        document.addEventListener("mousemove", onDrag);
        document.addEventListener("mouseup", stopDrag);
    };

    const onDrag = (event: MouseEvent) => {
        if (isDragging) {
            panelX = event.clientX - startX;
            panelY = event.clientY - startY;
        }
    };

    const stopDrag = () => {
        isDragging = false;
        document.removeEventListener("mousemove", onDrag);
        document.removeEventListener("mouseup", stopDrag);
    };

    listen("componentsUpdate", async () => {
        components = await getAllComponents();
    });
</script>

<div class="editor" style="zoom: {zoom}%; left: {panelX}px; top: {panelY}px;" on:mousedown={startDrag}>
    <!-- List of all available component factories -->
    {#each componentFactories as factory}
        <div class="factory-group">
            <h3>{factory.name}</h3>
            <ul>
                {#each factory.components as componentName}
                    <li>
                        {componentName}
                        <button on:click={() => createComponent(factory.name, componentName)}>+</button>
                    </li>
                {/each}
            </ul>
        </div>
    {/each}

    <!-- List of currently existing components -->
    <div class="existing-components">
        <h3>Existing Components</h3>
        <ul>
            {#each components as component}
                <li>
                    {component.name}
                    <button on:click={() => deleteComponent(component.id)}>-</button>
                </li>
            {/each}
        </ul>
    </div>
</div>

<style lang="scss">
  .editor {
    overflow: auto;
    position: absolute;
    background-color: rgba(0, 0, 0, 0.8); /* Black background */
    color: white; /* White text */
    padding: 1rem;
    width: 250px;
    height: 400px;
    border-radius: 8px;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
    cursor: move;
  }

  .factory-group, .existing-components {
    margin-bottom: 1rem;
  }

  h3 {
    font-size: 0.9rem;
    margin-bottom: 0.5rem;
    color: white;
  }

  ul {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  li {
    display: flex;
    justify-content: space-between;
    margin-bottom: 0.5rem;
  }

  button {
    background-color: #4677ff; /* Accent color */
    color: white;
    border: none;
    padding: 0.5rem 0.75rem;
    cursor: pointer;
    font-size: 0.8rem;
    margin-left: 15px;
    border-radius: 4px;
    transition: background-color 0.2s;
  }

  button:hover {
    background-color: #365bb8; /* Darker on hover */
  }
</style>
