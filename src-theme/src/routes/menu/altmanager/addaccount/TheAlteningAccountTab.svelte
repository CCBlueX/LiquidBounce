<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";

    let token = "";
    $: disabled = validateToken(token);

    function validateToken(token: string) {
        return token.length === 0;
    }
</script>

<Tab>
    <IconTextInput icon="exit" title="Token" bind:value={token}/>
    <ButtonSetting {disabled} title="Add Account" />
</Tab>