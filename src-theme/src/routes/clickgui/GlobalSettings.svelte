<script lang="ts">
    import {onMount} from "svelte";
    import type {ConfigurableSetting} from "../../integration/types";
    import {getGlobalSettings, setGlobalSettings} from "../../integration/rest";
    import GenericSetting from "./setting/common/GenericSetting.svelte";

    let globalSettings = $state<ConfigurableSetting | null>(null);

    async function fetchGlobalSettings() {
        globalSettings = await getGlobalSettings();
    }

    async function updateGlobalSettings() {
        if (!globalSettings) return;

        await setGlobalSettings($state.snapshot(globalSettings));
        await fetchGlobalSettings();
    }

    onMount(() => {
        fetchGlobalSettings();
    });
</script>

{#if globalSettings}
    <div class="settings">
        {#each globalSettings.value as _, i (globalSettings.value[i].name)}
            <GenericSetting
                    path="clickgui.settings"
                    bind:setting={globalSettings.value[i]}
                    on:change={updateGlobalSettings}
            />
        {/each}
    </div>
{/if}

<style lang="scss">
  .settings {
    background-color: var(--clickgui-module-settings-background-color);
    padding: 0 11px;
  }
</style>
