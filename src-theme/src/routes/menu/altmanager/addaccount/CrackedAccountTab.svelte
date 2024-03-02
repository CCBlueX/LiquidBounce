<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {createEventDispatcher} from "svelte";
    import {addCrackedAccount} from "../../../../integration/rest";

    const dispatch = createEventDispatcher();

    let username = "";

    async function addAccount() {
        await addCrackedAccount(username);
        dispatch("modify");
    }
</script>

<Tab>
    <IconTextInput icon="exit" title="Username" bind:value={username}/>
    <ButtonSetting title="Add Account" on:click={addAccount}/>
</Tab>