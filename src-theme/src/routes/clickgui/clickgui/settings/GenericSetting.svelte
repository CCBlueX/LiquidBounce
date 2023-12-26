<script>
    import BooleanSetting from "./BooleanSetting.svelte";
    import ColorSetting from "./ColorSetting.svelte";
    import RangeSetting from "./RangeSetting.svelte";
    import TextSetting from "./TextSetting.svelte";
    import TogglableSetting from "./TogglableSetting.svelte";
    import ChooseSetting from "./ChooseSetting.svelte";
    import ChoiceSetting from "./ChoiceSetting.svelte";
    import ConfigurableSetting from "./ConfigurableSetting.svelte";

    /**
     * A reference to the value instance of this setting. It is part of the module configurable and should NOT lose its reference.
     */
    export let reference;
    /**
     * This function is passed from the parent component and is used to write the new configurable to the client.
     * This will result in a request to the server.
     */
    export let write;

    let type = reference.valueType;
</script>

{#if type === "BOOLEAN"}
    <BooleanSetting reference={reference} write={write} />
{:else if type === "CHOOSE"}
    <ChooseSetting reference={reference} write={write} />
{:else if type === "TOGGLEABLE"}
    <TogglableSetting reference={reference} write={write} />
{:else if type === "INT" || type === "INT_RANGE" || type === "FLOAT" || type === "FLOAT_RANGE"}
    <RangeSetting reference={reference} write={write} />
{:else if type === "CHOICE"}
    <ChoiceSetting reference={reference} write={write} />
{:else if type === "TEXT"}
    <TextSetting reference={reference} write={write} />
{:else if type === "COLOR"}
    <ColorSetting reference={reference} write={write} />
{:else if type === "CONFIGURABLE"}
    <ConfigurableSetting reference={reference} write={write} />
{/if}


