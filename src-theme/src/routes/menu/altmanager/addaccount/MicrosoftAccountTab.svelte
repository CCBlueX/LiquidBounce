<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addMicrosoftAccount, addMicrosoftAccountCopyUrl} from "../../../../integration/rest.js";
    import {createEventDispatcher} from "svelte";

    const dispatch = createEventDispatcher();

    async function addAccount() {
        await addMicrosoftAccount();
        dispatch("modify");
    }

    async function copyLoginUrl() {
        await addMicrosoftAccountCopyUrl();
        dispatch("modify");
    }
</script>

<Tab>
    <ButtonSetting title="Link Account" on:click={addAccount}/>
    <ButtonSetting title="Copy URL" secondary={true} on:click={copyLoginUrl}/>
</Tab>