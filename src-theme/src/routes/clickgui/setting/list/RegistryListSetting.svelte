<script lang="ts">
    import type {ModuleSetting, NamedItem, RegistryItem, RegistryListSetting} from "../../../../integration/types";
    import GenericListSetting from "./GenericListSetting.svelte";
    import {onMount} from "svelte";
    import {getRegistryItems} from "../../../../integration/rest";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as RegistryListSetting;
    let items: NamedItem[] = [];

    onMount(async () => {
        const registryItems = await getRegistryItems(cSetting.registry);
        items = Object.entries(registryItems)
            .map(([identifier, item]) => ({
                value: identifier,
                name: item.name,
                icon: item.icon
            })) as NamedItem[];
        items = items.sort((a, b) => a.value.localeCompare(b.value));
    });
</script>

<GenericListSetting {path} bind:setting={setting} {items} on:change />
