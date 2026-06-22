<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addCrackedAccount, randomUsername} from "../../../../integration/rest";
    import IconButton from "../../common/buttons/IconButton.svelte";
    import SwitchSetting from "../../common/setting/SwitchSetting.svelte";

    let username = "";
    let online = false;

    async function addAccount() {
        await addCrackedAccount(username, online);
    }

    async function generateRandomUsername() {
        username = await randomUsername();
    }
</script>

<Tab>
    <IconTextInput icon="user" title="Username" bind:value={username} maxLength={16}>
        <IconButton icon="random" title="Random" on:click={generateRandomUsername}/>
    </IconTextInput>
    <SwitchSetting title="Use online UUID" bind:value={online}/>
    <ButtonSetting title="Add Account" on:click={addAccount} listenForEnter={true} inset={true}/>
</Tab>
