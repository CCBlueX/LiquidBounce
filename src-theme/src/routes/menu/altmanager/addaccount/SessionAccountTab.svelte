<script>
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";

    let sessionId = "";
</script>

<Tab>
    <IconTextInput icon="exit" title="Session ID" bind:value={sessionId}/>
    <ButtonSetting title="Add Account" />
</Tab>