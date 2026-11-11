<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ChooseSetting, ModuleSetting,} from "../../../integration/types";
    import ChoicePicker from "./common/ChoicePicker.svelte";
    import SettingRow from "./common/SettingRow.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as ChooseSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }
</script>

<SettingRow {setting}>
    <ChoicePicker options={cSetting.choices} bind:value={cSetting.value} on:change={handleChange}/>
</SettingRow>
