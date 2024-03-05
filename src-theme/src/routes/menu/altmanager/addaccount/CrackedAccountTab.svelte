<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {createEventDispatcher} from "svelte";
    import {addCrackedAccount} from "../../../../integration/rest";

    const dispatch = createEventDispatcher();

    let username = "";
    $: disabled = validateUsername(username);

    async function addAccount() {
        await addCrackedAccount(username);
        dispatch("modify");
    }

    function validateUsername(username: string): boolean {
        return !/^[a-zA-Z0-9_]{1,16}$/.test(username);
    }
</script>

<Tab>
    <IconTextInput icon="user" title="Username" bind:value={username} maxLength={16}/>
    <ButtonSetting {disabled} title="Add Account" on:click={addAccount}/>
</Tab>