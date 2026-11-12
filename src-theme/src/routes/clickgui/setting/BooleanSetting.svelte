<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type {
        ModuleSetting,
        BooleanSetting,
    } from "../../../integration/types";
    import Switch from "./common/Switch.svelte";
    import SettingRow from "./common/SettingRow.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as BooleanSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = { ...cSetting };

        dispatch("change");
    }
</script>

<SettingRow {setting}>
    <Switch bind:value={cSetting.value} on:change={handleChange}/>
</SettingRow>
