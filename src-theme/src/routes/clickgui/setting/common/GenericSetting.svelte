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
    import KeySetting from "../KeySetting.svelte";
    import BlocksSetting from "../blocks/BlocksSetting.svelte";

    export let setting: ModuleSetting;
    export let path: string;
</script>

<div>
    {#if setting.valueType === "BOOLEAN"}
        <BooleanSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "CHOICE"}
        <ChoiceSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "CHOOSE"}
        <ChooseSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "TOGGLEABLE"}
        <TogglableSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "INT"}
        <IntSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "INT_RANGE"}
        <IntRangeSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "FLOAT"}
        <FloatSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "FLOAT_RANGE"}
        <FloatRangeSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "CONFIGURABLE"}
        <ConfigurableSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "COLOR"}
        <ColorSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "TEXT"}
        <TextSetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "KEY"}
        <KeySetting {path} bind:setting={setting} on:change/>
    {:else if setting.valueType === "BLOCKS"}
        <BlocksSetting {path} bind:setting={setting} on:change/>
    {:else}
        <div style="color: white">Unsupported setting {setting.valueType}</div>
    {/if}
</div>
