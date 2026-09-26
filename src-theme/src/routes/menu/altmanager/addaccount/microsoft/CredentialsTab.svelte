<script lang="ts">
    import ButtonSetting from "../../../common/setting/ButtonSetting.svelte";
    import Tab from "../../../common/modal/Tab.svelte";
    import IconTextInput from "../../../common/setting/IconTextInput.svelte";
    import {addMicrosoftAccountCredentials} from "../../../../../integration/rest";

    let email = "";
    let password = "";
    let loading = false;
    $: disabled = email.length === 0 || password.length === 0;

    async function addAccount() {
        if (disabled) {
            return;
        }
        loading = true;
        await addMicrosoftAccountCredentials(email, password);
    }
</script>

<Tab>
    <IconTextInput title="E-Mail" icon="user" bind:value={email}></IconTextInput>
    <IconTextInput title="Password" icon="lock" type="password" bind:value={password}></IconTextInput>
    <ButtonSetting {disabled} title="Link Account" on:click={addAccount} listenForEnter={true} inset={true} {loading}/>
</Tab>
