<script lang="ts">
  import ClickGuiTabs from "./ClickGuiTabs.svelte";
  import { onMount } from "svelte";
  import { getGameWindow, getModuleSettings } from "../../integration/rest";
  import Description from "./modules/Description.svelte";
  import { fade } from "svelte/transition";
  import { listen } from "../../integration/ws";
  import type {
    ClickGuiScaleChangeEvent,
    ScaleFactorChangeEvent,
  } from "../../integration/events";
  import Modules from "./modules/Modules.svelte";
  import HudEditor from "./Hud/HudEditor.svelte";

  let minecraftScaleFactor = 2;
  let clickGuiScaleFactor = 1;
  $: scaleFactor = minecraftScaleFactor * clickGuiScaleFactor;
  $: zoom = scaleFactor * 50;

  onMount(async () => {
    const gameWindow = await getGameWindow();
    minecraftScaleFactor = gameWindow.scaleFactor;

    const clickGuiSettings = await getModuleSettings("ClickGUI");
    clickGuiScaleFactor =
      (clickGuiSettings.value.find((v) => v.name === "Scale")
        ?.value as number) ?? 1;
  });

  listen("scaleFactorChange", (e: ScaleFactorChangeEvent) => {
    minecraftScaleFactor = e.scaleFactor;
  });

  listen("clickGuiScaleChange", (e: ClickGuiScaleChangeEvent) => {
    clickGuiScaleFactor = e.value;
  });

  let activeTab: string;
</script>

<div
  class="clickgui"
  transition:fade|global={{ duration: 200 }}
  style="zoom: {zoom}%; width: {(2 / scaleFactor) * 100}vw; height: {(2 /
    scaleFactor) *
    100}vh;"
>
  <Description />
  <ClickGuiTabs bind:activeTab />
  {#if activeTab === "Modules"}
    <Modules {scaleFactor} />
  {:else if activeTab === "Settings"}
    settings
  {:else if activeTab === "Hud"}
    <HudEditor {scaleFactor} />
  {/if}
  <slot></slot>
</div>

<style lang="scss">
  @import "../../colors.scss";

  .clickgui {
    background-color: rgba($clickgui-base-color, 0.6);
    overflow: hidden;
    position: relative;
    will-change: opacity;
  }
</style>
