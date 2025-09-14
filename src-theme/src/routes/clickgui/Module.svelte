<script lang="ts">
    import {getContext} from "svelte";
    import {get, type Writable} from "svelte/store";
    import {setItem} from "../../integration/persistent_storage";
    import type {ConfigurableSetting} from "../../integration/types";
    import {onMount} from "svelte";
    import {getModuleSettings, setModuleSettings, setModuleEnabled} from "../../integration/rest";
    import GenericSetting from "./setting/common/GenericSetting.svelte";
    import {
        description as descriptionStore,
        highlightModuleName,
        scaleFactor,
        moduleAutoCollapse
    } from "./clickgui_store";
    import {convertToSpacedString, spaceSeperatedNames} from "../../theme/theme_config";

    export let name: string;
    export let enabled: boolean;
    export let description: string;
    export let aliases: string[];
    const expandedModuleName: Writable<string | null> = getContext("expandedModuleName");
    const modulesElement = getContext<HTMLElement>("modules-element");
    const path = `clickgui.${name}`;
    let expanded: boolean = false;
    let configurable: ConfigurableSetting;
    let moduleNameElement: HTMLElement;
    let hasSettings = false;
    $: if ($moduleAutoCollapse) {
        expanded = $expandedModuleName === name;
    }
    highlightModuleName.subscribe((name) => {
        if (!modulesElement || !name) return;

        const el = modulesElement.querySelector(`[data-module-name="${name}"]`);
        if (el instanceof HTMLElement) {
            console.log('scrolling to:', el.offsetTop);
        }
        setTimeout(() => {
            if (!moduleNameElement) {
                return;
            }
            moduleNameElement.scrollIntoView({
                behavior: "smooth",
                block: "center",
            });
        }, 1000);
    });

    async function fetchModuleSettings() {
        configurable = await getModuleSettings(name);
        hasSettings = configurable.value.filter(v => v.name !== "Bind" && v.name !== "Hidden").length > 0;
    }

    async function updateModuleSettings() {
        await setModuleSettings(name, configurable);
        await fetchModuleSettings();
    }

    async function toggleModule() {
        await setModuleEnabled(name, !enabled);
    }

    function setDescription() {
        if (!moduleNameElement) return;

        const boundingRect = moduleNameElement.getBoundingClientRect();
        const y = (boundingRect.top + (moduleNameElement.clientHeight / 2)) * (2 / $scaleFactor);

        let moduleDescription = description;
        if (aliases.length > 0) {
            moduleDescription += ` (aka ${aliases.map(name => $spaceSeperatedNames ? convertToSpacedString(name) : name).join(", ")})`;
        }


        if (window.innerWidth - boundingRect.right > 300) {
            const x = boundingRect.right * (2 / $scaleFactor);
            descriptionStore.set({
                x,
                y,
                anchor: "right",
                description: moduleDescription
            });
        } else {
            const x = boundingRect.left * (2 / $scaleFactor);

            descriptionStore.set({
                x,
                y,
                anchor: "left",
                description: moduleDescription
            });
        }
    }

    async function toggleExpanded() {
        if ($moduleAutoCollapse) {
            const panelId = moduleNameElement.closest("[data-panel-id]")?.getAttribute("data-panel-id");

            if (get(expandedModuleName) === name) {
                expandedModuleName.set(null);
                await setItem(path, "false");
            } else {

                document.querySelectorAll(`[data-panel-id="${panelId}"] [data-module-name]`).forEach(el => {
                    const key = `clickgui.${el.getAttribute("data-module-name")}`;
                    localStorage.setItem(key, "false");
                })

                expandedModuleName.set(name);
                await setItem(path, "true");
            }
        } else {

            expanded = !expanded;
            await setItem(path, expanded.toString());
        }
    }


    onMount(async () => {
        await fetchModuleSettings();

        setTimeout(() => {
            if ($moduleAutoCollapse) {
                document.querySelectorAll("[data-panel-id]").forEach(panel => {
                    const firstModule = panel.querySelector<HTMLElement>("[data-module-name]");
                    if (firstModule) {
                        const firstName = firstModule.getAttribute("data-module-name");
                        if (firstName) {
                            localStorage.setItem(`clickgui.${firstName}`, "true");
                            if (firstName === name) expanded = true;
                        }
                    }
                });
            } else {
                expanded = localStorage.getItem(path) === "true";
            }
        }, 500);
    });
</script>

<div
        class="module"
        class:expanded
        class:has-settings={hasSettings}
        data-module-name={name}

>
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
            bind:this={moduleNameElement}
            class="name"
            class:enabled
            class:highlight={name === $highlightModuleName}
            on:click={toggleModule}
            on:contextmenu|preventDefault={toggleExpanded}
            on:mouseenter={setDescription}
            on:mouseleave={() => descriptionStore.set(null)}
    >
  <span class="name-inner">
 {$spaceSeperatedNames ? convertToSpacedString(name) : name}
  </span>
    </div>

    {#if expanded && configurable}
        <div class="settings-wrapper">
            <div class="settings">
                {#each configurable.value as setting (setting.name)}
                    <GenericSetting {path} bind:setting on:change={updateModuleSettings}/>
                {/each}
            </div>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../colors.scss" as *;

  @keyframes shake-horizontal {
    0%, 100% {
      transform: translateX(0);
    }
    10%, 90% {
      transform: translateX(-4px);
    }
    20%, 80% {
      transform: translateX(4px);
    }
    30%, 70% {
      transform: translateX(-6px);
    }
    40%, 60% {
      transform: translateX(6px);
    }
    50% {
      transform: translateX(0);
    }
  }

  @keyframes highlight-flash {
    0% {
      box-shadow: 0 0 0 color-mix(in srgb, var(--primary-color) 60%, transparent),
      0 0 10px color-mix(in srgb, var(--primary-color) 40%, transparent);
      opacity: 1;
    }
    50% {
      box-shadow: 0 0 12px color-mix(in srgb, var(--primary-color) 80%, transparent),
      0 0 24px color-mix(in srgb, var(--primary-color) 60%, transparent);
      opacity: 0.7;
    }
    100% {
      box-shadow: 0 0 0 color-mix(in srgb, var(--primary-color) 60%, transparent),
      0 0 10px color-mix(in srgb, var(--primary-color) 40%, transparent);
      opacity: 1;
    }
  }

  .module {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    position: relative;
    overflow-y: auto;
    max-height: 100%;

    &.expanded {
      overflow: visible;
      transition: all 0.3s ease-out;
    }

    &.has-settings {
      .name::after {
        content: "";
        display: block;
        position: absolute;
        height: 10px;
        width: 10px;
        right: 15px;
        top: 50%;
        background-image: url("/img/clickgui/icon-settings-expand.svg");
        background-position: center;
        background-repeat: no-repeat;
        opacity: 0.5;
        transform-origin: 50% 50%;
        transform: translateY(-50%) rotate(-90deg);
        transition: ease opacity 0.2s,
        ease transform 0.4s;
      }

      &.expanded .name::after {
        transform: translateY(-50%) rotate(0);
        opacity: 1;
      }
    }

    .settings-wrapper {
      background: rgba($base, 0.3);
      border-radius: 14px;
      padding: 8px 10px;
      position: relative;
      box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color) 20%, transparent),
      0 0 8px color-mix(in srgb, var(--primary-color) 10%, transparent),
      inset 0 0 4px color-mix(in srgb, var(--primary-color) 5%, transparent);
      transition: box-shadow 0.3s ease;

      &:hover {
        box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color) 40%, transparent),
        0 0 12px color-mix(in srgb, var(--primary-color) 30%, transparent),
        inset 0 0 6px color-mix(in srgb, var(--primary-color) 15%, transparent);
      }
    }

    .name {
      cursor: pointer;
      text-align: center;
      font-size: calc(var(--font-size) + 2px);
      font-weight: 500;
      padding: 10px;
      color: rgba(150, 150, 150);
      position: relative;
      overflow: hidden;
      transition: all 0.3s ease-out;

      .name-inner {
        display: inline-block;
      }

      &.highlight .name-inner {
        animation: shake-horizontal 0.2s linear 3;
      }

      &:hover {
        background: rgba($base, 0.2);
        color: gray;
      }

      &.enabled {
        background: linear-gradient(135deg in oklch,
                color-mix(in srgb, var(--primary-color) 30%, transparent) 0%,
                color-mix(in srgb, var(--secondary-color) 30%, transparent) 51%,
                color-mix(in srgb, var(--primary-color) 30%, transparent) 100%
        );
        background-size: 200% auto;
        color: white;
        text-shadow: 0 0 12px color-mix(in srgb, var(--primary-color) 70%, transparent);
        box-shadow: 0 0 8px color-mix(in srgb, var(--primary-color) 30%, transparent),
        inset 0 0 10px rgba(white, 0.1);
        transition: background-position 0.3s ease;

        &:hover {
          text-shadow: 0 0 16px color-mix(in srgb, var(--primary-color) 90%, transparent);
          background-position: bottom right;
        }

        &:active {
          transform: scale(0.98);
          transition-duration: 0.2s;
        }
      }

      &.highlight {
        color: white;
        background: linear-gradient(135deg in oklch,
                color-mix(in srgb, var(--secondary-color) 50%, transparent),
                color-mix(in srgb, var(--primary-color) 50%, transparent));
        animation: highlight-flash 1.2s ease-out 1;

        &::after {
          content: "";
          position: absolute;
          top: 50%;
          left: 50%;
          width: 100%;
          height: 100%;
          background: radial-gradient(circle,
                  color-mix(in srgb, var(--primary-color) 30%, transparent),
                  transparent 70%);
          transform: translate(-50%, -50%) scale(0);
          border-radius: 50%;
          transition: transform 0.4s ease-out, opacity 0.4s ease-out;
          opacity: 0.6;
          pointer-events: none;
        }

        &:hover::after {
          transform: translate(-50%, -50%) scale(1.2);
          opacity: 0.2;
          transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.4, 1), opacity 0.3s ease;
        }
      }
    }
  }
</style>

