<script lang="ts">
    import {untrack} from "svelte";
    import type {ChoiceSetting, ModuleSetting} from "../../../integration/types";
    import GenericSetting from "./common/GenericSetting.svelte";
    import {followTrail, startPage} from "./pages";

    /**
     * The settings of the group at [trail] as a page. It is created anew for every page: settings learn their
     * place when they are created.
     */
    let {root = $bindable(), trail, path, exclude = [], onchange}: {
        root: ModuleSetting;
        trail: string[];
        path: string;
        exclude?: string[];
        onchange: () => void;
    } = $props();

    startPage(untrack(() => trail));

    const group = $derived(followTrail(root, trail).at(-1)!);

    // The list inside the tree and not a copy, so that a change lands in the root
    const settings = $derived(group.valueType === "CHOICE"
        ? (group as ChoiceSetting).choices[(group as ChoiceSetting).active].value as ModuleSetting[]
        : group.value as ModuleSetting[]);
</script>

{#each settings as setting, index (setting.name)}
    <!-- A toggleable group starts with its switch, which the header of the page shows -->
    {#if !exclude.includes(setting.name) && !(group.valueType === "TOGGLEABLE" && index === 0)}
        <GenericSetting {path} bind:setting={settings[index]} on:change={onchange}/>
    {/if}
{/each}
