<script>
    import BooleanSetting from "./BooleanSetting.svelte";
    import ColorSetting from "./ColorSetting.svelte";
    import RangeSetting from "./RangeSetting.svelte";
    import TextSetting from "./TextSetting.svelte";
    import TogglableSetting from "./TogglableSetting.svelte";
    import ChooseSetting from "./ChooseSetting.svelte";
    import ChoiceSetting from "./ChoiceSetting.svelte";
    import ConfigurableSetting from "./ConfigurableSetting.svelte";

    export let instance;
    export let write;

    let type = instance.valueType;
</script>

{#if type === "BOOLEAN"}
    <BooleanSetting {instance} write={write} />
{:else if type === "CHOOSE"}
    <ChooseSetting {instance} write={write} />
{:else if type === "TOGGLEABLE"}
    <TogglableSetting {instance} write={write} />
{:else if type === "INT" || type === "INT_RANGE" || type === "FLOAT" || type === "FLOAT_RANGE"}
    <RangeSetting {instance} write={write} />
{:else if type === "CHOICE"}
    <ChoiceSetting {instance} write={write} />
{:else if type === "TEXT"}
    <TextSetting {instance} write={write} />
{:else if type === "COLOR"}
    <ColorSetting {instance} write={write} />
{:else if type === "CONFIGURABLE"}
    <ConfigurableSetting {instance} write={write} />
{/if}


