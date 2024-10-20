<script lang="ts">
    import type {ModuleSetting} from "../../../../integration/types";
    import BooleanSetting from "../BooleanSetting.svelte";
    import ChoiceSetting from "../ChoiceSetting.svelte";
    import ChooseSetting from "../ChooseSetting.svelte";
    import ConfigurableSetting from "../ConfigurableSetting.svelte";
    import FloatRangeSetting from "../FloatRangeSetting.svelte";
    import FloatSetting from "../FloatSetting.svelte";
    import IntRangeSetting from "../IntRangeSetting.svelte";
    import IntSetting from "../IntSetting.svelte";
    import TogglableSetting from "../TogglableSetting.svelte";
    import ColorSetting from "../ColorSetting.svelte";
    import TextSetting from "../TextSetting.svelte";
    import BlocksSetting from "../blocks/BlocksSetting.svelte";
    import {slide} from "svelte/transition";
    import {onMount} from "svelte";
    import TextArraySetting from "../TextArraySetting.svelte";
    import BindSetting from "../BindSetting.svelte";

    export let setting: ModuleSetting;
    export let path: string;
    export let skipAnimationDelay = false;

    let ready = skipAnimationDelay;

    onMount(() => {
        setTimeout(() => {
            ready = true;
        }, 200)
    });
</script>

{#if ready}
    <div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}}>
        {#if setting.valueType === "BOOLEAN"}
            <BooleanSetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "CHOICE"}
            <ChoiceSetting {path} bind:setting={setting} on:change/>
        {:else if setting.valueType === "CHOOSE"}
            <ChooseSetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "TOGGLEABLE"}
            <TogglableSetting {path} bind:setting={setting} on:change/>
        {:else if setting.valueType === "INT"}
            <IntSetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "INT_RANGE"}
            <IntRangeSetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "FLOAT"}
            <FloatSetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "FLOAT_RANGE"}
            <FloatRangeSetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "CONFIGURABLE"}
            <ConfigurableSetting {path} bind:setting={setting} on:change/>
        {:else if setting.valueType === "COLOR"}
            <ColorSetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "TEXT"}
            <TextSetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "BLOCKS"}
            <BlocksSetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "TEXT_ARRAY"}
            <TextArraySetting bind:setting={setting} on:change/>
        {:else if setting.valueType === "BIND"}
            <BindSetting bind:setting={setting} on:change/>
        {:else}
            <div style="color: white">Unsupported setting {setting.valueType}</div>
        {/if}
    </div>
{/if}