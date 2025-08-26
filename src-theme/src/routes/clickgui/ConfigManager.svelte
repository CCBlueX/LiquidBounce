<script lang="ts">
    import { onMount } from "svelte";
    import { getConfigs, loadConfig, saveConfig, deleteConfig } from "../../integration/rest";
    import { getGameWindow } from "../../integration/rest";
    import type {ScaleFactorChangeEvent} from "../../integration/events";
    import {listen} from "../../integration/ws";

    let configs: string[] = [];
    let selectedConfig: string | null = null;
    let panelElement: HTMLElement;
    let moving = false;
    let offsetX = 0;
    let offsetY = 0;
    let panelConfig = { top: 20, left: 20, zIndex: 0 };
    let windowWidth = window.innerWidth;
    let windowHeight = window.innerHeight;
    let panelWidth = 0;
    let panelHeight = 0;
    let isVisible = false;
    let scaleFactor = 1;
    let minecraftScaleFactor = 2;
    let clickGuiScaleFactor = 1;

    $: scaleFactor = minecraftScaleFactor * clickGuiScaleFactor;

    async function updateScale() {
        const gameWindow = await getGameWindow();
        minecraftScaleFactor = gameWindow.scaleFactor; // 使用游戏窗口的 scaleFactor
        scaleFactor = minecraftScaleFactor * clickGuiScaleFactor;
        scaleFactor = Math.min(window.innerWidth / gameWindow.width, window.innerHeight / gameWindow.height) * scaleFactor;
    }
    function toggleVisibility() {
        isVisible = !isVisible;
    }

    onMount(() => {
        (async () => {
            try {
                configs = await getConfigs();
                console.log("Configs loaded:", configs);
                const savedConfig = localStorage.getItem("config-manager.position");
                if (savedConfig) {
                    const { top, left, zIndex } = JSON.parse(savedConfig);
                    panelConfig = { top, left, zIndex };
                }
                await updateScale();
            } catch (error) {
                console.error("Failed to fetch configs:", error);
            }
        })();
        const resizeHandler = () => {
            updateDimensions();
            updateScale();
        };
        window.addEventListener('resize', resizeHandler);
        const updateDimensions = async () => {
            windowWidth = window.innerWidth;
            windowHeight = window.innerHeight;
            panelWidth = panelElement?.offsetWidth * scaleFactor || 0;
            panelHeight = panelElement?.offsetHeight * scaleFactor || 0;
            fixPosition();
        };
        updateDimensions();
        window.addEventListener('resize', updateDimensions);
        window.addEventListener('mousemove', onMouseMove);
        window.addEventListener('mouseup', onMouseUp);
        window.addEventListener('keydown', (e) => {
            if (e.key === '`') {
                toggleVisibility();
            }
        });
        return () => {
            window.removeEventListener('resize', updateDimensions);
            window.removeEventListener('mousemove', onMouseMove);
            window.removeEventListener('mouseup', onMouseUp);
            window.removeEventListener('keydown', toggleVisibility);
        };
    });

    async function savePanelConfig() {
        localStorage.setItem(
            "config-manager.position",
            JSON.stringify({ top: panelConfig.top, left: panelConfig.left, zIndex: panelConfig.zIndex })
        );
    }

    function onMouseDown(e: MouseEvent) {
        moving = true;
        offsetX = e.clientX - panelConfig.left;
        offsetY = e.clientY - panelConfig.top;
        panelConfig.zIndex = 1000;
        panelElement.classList.add("no-transition");
    }

    function onMouseMove(e: MouseEvent) {
        if (!moving) return;
        panelConfig.left = e.clientX - offsetX;
        panelConfig.top = e.clientY - offsetY;
        fixPosition();
    }

    function onMouseUp() {
        moving = false;
        panelElement.classList.remove("no-transition");
        savePanelConfig();
    }

    function fixPosition() {
        panelConfig.left = Math.max(0, Math.min(panelConfig.left, windowWidth - panelWidth));
        panelConfig.top = Math.max(0, Math.min(panelConfig.top, windowHeight - panelHeight));
    }

    const handleLoad = async (name: string) => {
        await loadConfig(name);
    };

    const handleSave = async () => {
        if (!selectedConfig) return;
        await saveConfig(selectedConfig);
        configs = await getConfigs();
    };

    const handleDelete = async (name: string) => {
        await deleteConfig(name);
        configs = await getConfigs();
    };
    listen("scaleFactorChange", (e: ScaleFactorChangeEvent) => {
        minecraftScaleFactor = e.scaleFactor;
    });

</script>

<svelte:window on:mousemove={onMouseMove} on:mouseup={onMouseUp} />

{#if isVisible}
    <div
            bind:this={panelElement}
            class="config-manager"
            class:no-transition={!moving}
            style="transform: translate({panelConfig.left}px, {panelConfig.top}px) scale({scaleFactor * 50}%); z-index: {panelConfig.zIndex};"
    >

        <div
                class="title"
                on:mousedown={onMouseDown}
                role="presentation"
                tabindex="-1"
        >
            <h2>Local Configs</h2>
        </div>

        <div class="configs-list">
            {#if configs.length > 0}
                <ul>
                    {#each configs as item (item)}
                        <li>
                            <span>{item}</span>
                            <button on:click={() => handleLoad(item)}>Load</button>
                            <button on:click={() => handleDelete(item)}>Delete</button>
                        </li>
                    {/each}
                </ul>
            {:else}
                <p>No configs available</p>
            {/if}
        </div>
        <input bind:value={selectedConfig} placeholder="Config name" />
        <button on:click={handleSave}>Save</button>
    </div>
{/if}

<style lang="scss">
  .config-manager {
    position: fixed;
    padding: 10px;
    background: #1a1a1a;
    border-radius: 5px;
    color: #ccc;
    width: 250px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.4);

    &.no-transition {
      transition: none !important;
    }
    .title {
      cursor: grab;
      padding: 5px;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 5px 5px 0 0;
      text-align: center;

      h2 {
        margin: 0;
        font-size: 16px;
      }
    }
    .configs-list {
      height: calc(10 * 35px);
      overflow-y: auto;
      margin-bottom: 10px;

      ul {
        list-style: none;
        padding: 0;
        margin: 0;
      }

      li {
        display: flex;
        align-items: center;
        height: 35px;

        span {
          flex-grow: 1;
          padding: 5px;
        }

        button {
          padding: 5px 10px;
          margin-left: 5px;
          background: #333;
          border: none;
          color: #fff;
          cursor: pointer;

          &:hover {
            background: #444;
          }
        }
      }


      p {
        margin: 10px 0;
        text-align: center;
      }

      button {
        padding: 5px 10px;
        margin-top: 5px;
        background: #333;
        border: none;
        color: #fff;
        cursor: pointer;

        &:hover {
          background: #444;
        }
      }
    }

    .configs-list::-webkit-scrollbar {
      width: 6px;
    }

    .configs-list::-webkit-scrollbar-track {
      background: #2a2a2a;
      border-radius: 3px;
    }

    .configs-list::-webkit-scrollbar-thumb {
      background: #555;
      border-radius: 3px;
    }

    .configs-list::-webkit-scrollbar-thumb:hover {
      background: #666;
    }
  }
</style>
