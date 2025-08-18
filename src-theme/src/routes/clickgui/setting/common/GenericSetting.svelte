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
    import {slide} from "svelte/transition";
    import BindSetting from "../BindSetting.svelte";
    import VectorSetting from "../VectorSetting.svelte";
    import KeySetting from "../KeySetting.svelte";
    import MultiChooseSetting from "../MultiChooseSetting.svelte";
    import MutableListSetting from "../list/MutableListSetting.svelte";
    import ItemListSetting from "../list/ItemListSetting.svelte";
    import RegistryListSetting from "../list/RegistryListSetting.svelte";

    export let setting: ModuleSetting;
    export let path: string;
</script>


<div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}}>
    {#if setting.valueType === "BOOLEAN"}
        <BooleanSetting bind:setting={setting} on:change/>
    {:else if setting.valueType === "CHOICE"}
        <ChoiceSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "CHOOSE"}
        <ChooseSetting bind:setting={setting} on:change/>
    {:else if setting.valueType === "MULTI_CHOOSE"}
        <MultiChooseSetting {path} bind:setting={setting} on:change/>
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
    {:else if setting.valueType === "MUTABLE_LIST" }
        <MutableListSetting bind:setting={setting} on:change/>
    {:else if setting.valueType === "ITEM_LIST" }
        <ItemListSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "REGISTRY_LIST" }
        <RegistryListSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "BIND"}
        <BindSetting bind:setting={setting} on:change/>
    {:else if setting.valueType === "VECTOR_I" || setting.valueType === "VECTOR_D" }
        <VectorSetting bind:setting={setting} on:change/>
    {:else if setting.valueType === "KEY"}
        <KeySetting bind:setting={setting} on:change/>
    {:else}
        <div style="color: white">Unsupported setting {setting.valueType}</div>
    {/if}
</div>
