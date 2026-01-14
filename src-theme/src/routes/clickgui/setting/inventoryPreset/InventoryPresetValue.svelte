<script lang="ts">
    import type {InventoryPresetValue, ModuleSetting} from "../../../../integration/types";
    import {spaceSeperatedNames} from "../../../../theme/theme_config";
    import {createEventDispatcher} from "svelte";
    import ItemPreview from "./ItemPreview.svelte";
    import PresetModal from "./config/PresetModal.svelte";

    export let setting: ModuleSetting;

    let configuring = false

    const cSetting = setting as InventoryPresetValue;

    $: preset = cSetting.value

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="setting">
    <div class="head">
        <span class="title">{$spaceSeperatedNames ? "Inventory Preset" : "InventoryPreset"}</span>
    </div>

    <div class="presets">
        <div class="preset" on:click={() => configuring = true}>
            {#each preset.items as group, idx (idx)}
                <ItemPreview bind:group />
            {/each}
        </div>
    </div>
</div>

{#if configuring}
    <PresetModal
            bind:preset
            on:close={() => configuring = false}
            on:change={handleChange}
    />
{/if}

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .preset {
    display: flex;
    justify-content: space-between;
    cursor: pointer;
  }

  .setting {
    padding: 7px 0;
    color: $clickgui-text-color;
  }

  .head {
    margin-bottom: 10px;
  }

  .presets {
    margin-bottom: 5px;
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  .title {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 600;
  }
</style>
