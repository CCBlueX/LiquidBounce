<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type {
        ModuleSetting,
        ConfigurableSetting,
    } from "../../../integration/types";
    import GenericSetting from "./common/GenericSetting.svelte";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as ConfigurableSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="title">{cSetting.name}</div>

    <div class="nested-settings">
        {#each cSetting.value as setting (setting.name)}
            <GenericSetting path="{path}.{cSetting.name}" bind:setting on:change={handleChange} />
        {/each}
    </div>
</div>

<style lang="scss">
    @import "../../../colors.scss";

    .setting {
        padding: 7px 0px;
    }

    .title {
        color: $clickgui-text-color;
        font-size: 12px;
        font-weight: 600;
    }
</style>
