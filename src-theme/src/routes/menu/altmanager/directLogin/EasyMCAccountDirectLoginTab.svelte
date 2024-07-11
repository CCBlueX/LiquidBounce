<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {browse, directLoginToEasyMCAccount} from "../../../../integration/rest";

    let token = "";
    $: disabled = validateToken(token);

    function validateToken(token: string) {
        return token.length === 0;
    }

    async function login() {
        if (disabled) {
            return;
        }
        await directLoginToEasyMCAccount(token);
    }
</script>

<Tab>
    <IconTextInput icon="user" title="Token" bind:value={token}/>
    <ButtonSetting title="Login" {disabled} on:click={login} listenForEnter={true} inset={true}/>
    <ButtonSetting title="Get Account Token" on:click={() => browse("EASYMC")} secondary={true}/>
</Tab>