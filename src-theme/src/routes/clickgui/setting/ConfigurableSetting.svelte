<script lang="ts">
    import type {ModuleSetting} from "../../../integration/types";
    import SettingGroup from "./common/SettingGroup.svelte";

    export let setting: ModuleSetting;
    export let path: string;
</script>

<SettingGroup {path} bind:setting on:change/>
