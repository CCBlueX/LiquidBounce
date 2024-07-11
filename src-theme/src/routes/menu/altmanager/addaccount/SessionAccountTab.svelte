<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addSessionAccount} from "../../../../integration/rest";

    let token = "";
    $: disabled = validateSessionId(token);

    function validateSessionId(token: string): boolean {
        return token.length === 0;
    }

    async function addAccount() {
        if (disabled) {
            return;
        }
        await addSessionAccount(token);
    }
</script>

<Tab>
    <IconTextInput icon="user" title="Session ID" bind:value={token}/>
    <ButtonSetting title="Add Account" {disabled} on:click={addAccount} listenForEnter={true} inset={true} />
</Tab>