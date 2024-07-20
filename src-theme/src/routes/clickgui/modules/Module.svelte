<script lang="ts">
  import { onMount } from "svelte";
  import {
    getModuleSettings,
    setModuleSettings,
    setModuleEnabled,
  } from "../../../integration/rest";
  import type { ConfigurableSetting } from "../../../integration/types";
  import GenericSetting from "../setting/common/GenericSetting.svelte";
  import { slide } from "svelte/transition";
  import { quintOut } from "svelte/easing";
  import {
    description as descriptionStore,
    highlightModuleName,
  } from "../clickgui_store";
  import { setItem } from "../../../integration/persistent_storage";
  import {
    convertToSpacedString,
    spaceSeperatedNames,
  } from "../../../theme/theme_config";

  export let name: string;
  export let enabled: boolean;
  export let description: string;
  export let aliases: string[];

  let moduleNameElement: HTMLElement;
  let configurable: ConfigurableSetting;
  const path = `clickgui.${name}`;
  let expanded = false;

  onMount(async () => {
    configurable = await getModuleSettings(name);

    setTimeout(() => {
      expanded = localStorage.getItem(path) === "true";
    }, 500);
  });

  highlightModuleName.subscribe(() => {
    if (name !== $highlightModuleName) {
      return;
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

  async function updateModuleSettings() {
    await setModuleSettings(name, configurable);
    configurable = await getModuleSettings(name);
  }

  async function toggleModule() {
    await setModuleEnabled(name, !enabled);
  }

  function setDescription() {
    const y =
      (moduleNameElement?.getBoundingClientRect().top ?? 0) +
      (moduleNameElement?.clientHeight ?? 0) / 2;
    const x = moduleNameElement?.getBoundingClientRect().right ?? 0;
    let moduleDescription = description;
    if (aliases.length > 0) {
      moduleDescription += ` (aka ${aliases.map((a) => ($spaceSeperatedNames ? convertToSpacedString(a) : a)).join(", ")})`;
    }
    descriptionStore.set({
      x,
      y,
      description: moduleDescription,
    });
  }

  async function toggleExpanded() {
    expanded = !expanded;
    await setItem(path, expanded.toString());
  }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
  class="module"
  class:expanded
  class:has-settings={configurable?.value.length > 2}
  in:slide={{ duration: 500, easing: quintOut }}
  out:slide={{ duration: 500, easing: quintOut }}
>
  <!-- svelte-ignore a11y-click-events-have-key-events -->
  <div
    class="name"
    on:contextmenu|preventDefault={toggleExpanded}
    on:click={toggleModule}
    on:mouseenter={setDescription}
    on:mouseleave={() => descriptionStore.set(null)}
    bind:this={moduleNameElement}
    class:enabled
    class:highlight={name === $highlightModuleName}
  >
    {#if $spaceSeperatedNames}
      {convertToSpacedString(name)}
    {:else}
      {name}
    {/if}
  </div>

  {#if expanded && configurable}
    <div class="settings">
      {#each configurable.value as setting (setting.name)}
        <GenericSetting
          skipAnimationDelay={true}
          {path}
          bind:setting
          on:change={updateModuleSettings}
        />
      {/each}
    </div>
  {/if}
</div>

<style lang="scss">
  @import "../../../colors.scss";

  .module {
    position: relative;

    .name {
      cursor: pointer;
      transition:
        ease background-color 0.2s,
        ease color 0.2s;

      color: $clickgui-text-dimmed-color;
      text-align: center;
      font-size: 12px;
      font-weight: 500;
      position: relative;
      padding: 10px;

      &.highlight::before {
        content: "";
        position: absolute;
        top: 0;
        left: 0;
        width: calc(100% - 4px);
        height: calc(100% - 4px);
        border: solid 2px $accent-color;
      }

      &:hover {
        background-color: rgba($clickgui-base-color, 0.85);
        color: $clickgui-text-color;
      }

      &.enabled {
        color: $accent-color;
      }
    }

    .settings {
      background-color: rgba($clickgui-base-color, 0.5);
      border-left: solid 4px $accent-color;
      padding: 0 11px 0 7px;
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
        transition:
          ease opacity 0.2s,
          ease transform 0.4s;
      }

      &.expanded .name::after {
        transform: translateY(-50%) rotate(0);
        opacity: 1;
      }
    }
  }
</style>
