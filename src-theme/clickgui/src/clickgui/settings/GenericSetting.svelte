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

    let type = instance.getValueType().toString();
</script>

{#if type === "BOOLEAN"}
    <BooleanSetting {instance}/>
{:else if type === "CHOOSE"}
    <ChooseSetting {instance}/>
{:else if type === "TOGGLEABLE"}
    <TogglableSetting {instance}/>
{:else if type === "INT" || type === "INT_RANGE" || type === "FLOAT" || type === "FLOAT_RANGE"}
    <RangeSetting {instance}/>
{:else if type === "CHOICE"}
    <ChoiceSetting {instance}/>
{:else if type === "CONFIGURABLE"}
    <ConfigurableSetting {instance}/>
{:else if type === "TEXT"}
    <TextSetting {instance}/>
{:else if type === "COLOR"}
    <ColorSetting {instance}/>
{/if}
