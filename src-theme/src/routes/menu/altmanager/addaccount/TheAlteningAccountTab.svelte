<script>
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";

    let token = "";
</script>

<Tab>
    <IconTextInput icon="exit" title="Token" bind:value={token}/>
    <ButtonSetting title="Add Account" />
</Tab>