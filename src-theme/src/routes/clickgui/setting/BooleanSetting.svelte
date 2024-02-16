<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type {
        ModuleSetting,
        BooleanSetting,
    } from "../../../integration/types";
    import Switch from "./common/Switch.svelte";

    export let setting: ModuleSetting;

    const dispatch = createEventDispatcher();

    const cSetting = setting as BooleanSetting;

    function handleChange() {
        setting = { ...cSetting };

        dispatch("change");
    }
</script>

<div class="setting">
    <Switch
        name={cSetting.name}
        bind:value={cSetting.value}
        on:change={handleChange}
    />
</div>

<style lang="scss">
    .setting {
        padding: 7px 0px;
    }
</style>
