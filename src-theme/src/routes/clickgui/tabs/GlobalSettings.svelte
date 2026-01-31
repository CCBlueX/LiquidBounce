<script lang="ts">
    import {onMount} from "svelte";
    import type {ConfigurableSetting as ConfigurableSettingData} from "../../../integration/types";
    import {getGlobalSettings, setGlobalSettings} from "../../../integration/rest";
    import ConfigurableSetting from "../setting/ConfigurableSetting.svelte";
    import WindowPanel from "./WindowPanel.svelte";

    let globalSettings = $state<ConfigurableSettingData | null>(null);

    async function fetchGlobalSettings() {
        globalSettings = await getGlobalSettings();
    }

    async function updateGlobalSettings() {
        if (!globalSettings) return;

        await setGlobalSettings($state.snapshot(globalSettings));
        await fetchGlobalSettings();
    }

    onMount(() => {
        void fetchGlobalSettings();
    });
</script>

<WindowPanel title="Global Settings" icon="client">
    <div class="settings-grid">
        {#if globalSettings}
            {#each globalSettings.value as _, i (globalSettings.value[i].name)}
                {#if globalSettings.value[i].valueType === "CONFIGURABLE" ||
                globalSettings.value[i].valueType === "TOGGLEABLE"}
                    <div class="setting-item">
                        <ConfigurableSetting
                                path="clickgui.global"
                                bind:setting={globalSettings.value[i]}
                                hideExpandControl={true}
                                on:change={updateGlobalSettings}
                        />
                    </div>
                {/if}
            {/each}
        {/if}
    </div>
</WindowPanel>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .settings-grid {
    column-count: 2;
    column-gap: 25px;
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
    margin-bottom: 15px;
  }
</style>
