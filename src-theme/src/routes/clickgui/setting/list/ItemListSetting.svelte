<script lang="ts">
    import type {ItemListSetting, ModuleSetting, NamedItem} from "../../../../integration/types";
    import GenericListSetting from "./GenericListSetting.svelte";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as ItemListSetting;
    let items: NamedItem[] = cSetting.items;
</script>

<GenericListSetting {path} bind:setting={setting} {items} on:change />
