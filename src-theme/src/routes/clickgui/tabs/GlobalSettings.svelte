<!--
  - This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
  -
  - Copyright (c) 2015 - 2026 CCBlueX
  -
  - LiquidBounce is free software: you can redistribute it and/or modify
  - it under the terms of the GNU General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - LiquidBounce is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  - GNU General Public License for more details.
  -
  - You should have received a copy of the GNU General Public License
  - along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
  -->

<script lang="ts">
    import {onMount} from "svelte";
    import type {ConfigurableSetting as ConfigurableSettingData} from "../../../integration/types";
    import {getGlobalSettings, setGlobalSettings} from "../../../integration/rest";
    import ConfigurableSetting from "../setting/ConfigurableSetting.svelte";
    import WindowPanel from "./WindowPanel.svelte";

    let globalSettings: ConfigurableSettingData | null = null;

    async function fetchGlobalSettings() {
        globalSettings = await getGlobalSettings();
    }

    async function updateGlobalSettings() {
        if (!globalSettings) {
            return;
        }

        await setGlobalSettings(globalSettings);
        await fetchGlobalSettings();
    }

    onMount(fetchGlobalSettings);

</script>

<WindowPanel title="Global Settings">
    <div class="settings-grid">
        {#each globalSettings?.value ?? [] as setting (setting.name)}
            {#if setting.valueType === "CONFIGURABLE" || setting.valueType === "TOGGLEABLE"}
                <div class="setting-item">
                    <ConfigurableSetting
                            path="clickgui.global"
                            bind:setting
                            hideExpandControl={true}
                            on:change={updateGlobalSettings}
                    />
                </div>
            {/if}
        {/each}
    </div>
</WindowPanel>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .settings-grid {
    column-count: 2;
    column-gap: 24px;
    column-rule: 1px solid rgba($clickgui-text-color, 0.12);
    column-fill: balance;
    overflow: visible;
  }

  @media (max-width: 900px) {
    .settings-grid {
      column-count: 1;
    }
  }

  .setting-item {
    break-inside: avoid;
    display: inline-block;
    width: 100%;
    border-bottom: 1px solid rgba($clickgui-text-color, 0.12);
    margin-bottom: 10px;
    padding-bottom: 10px;
  }

  .setting-item:last-child,
  // hilfe, wie bekomme ich die linie auf der linken seite weg?
  //.setting-item:nth-last-child(3):nth-child(even) {
  {
    border-bottom: none;
    padding-bottom: 0;
  }
</style>
