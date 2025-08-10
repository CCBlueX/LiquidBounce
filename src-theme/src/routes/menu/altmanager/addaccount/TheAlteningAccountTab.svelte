<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addAlteningAccount, browse} from "../../../../integration/rest";

    let token = "";
    let loading = false;
    $: disabled = validateToken(token);

    function validateToken(token: string) {
        return token.length === 0;
    }

    async function addAccount() {
        if (disabled) {
            return;
        }
        loading = true;
        await addAlteningAccount(token);
    }
</script>

<Tab>
    <IconTextInput icon="user" title="Token" bind:value={token}/>
    <ButtonSetting {disabled} title="Add Account" on:click={addAccount} listenForEnter={true} inset={true} {loading}/>
    <ButtonSetting title="Get Account Token" on:click={() => browse("THE_ALTENING")} secondary={true}/>
</Tab>
