<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addMicrosoftAccount, addMicrosoftAccountCopyUrl} from "../../../../integration/rest.js";

    async function addAccount() {
        await addMicrosoftAccount();
    }

    async function copyLoginUrl() {
        await addMicrosoftAccountCopyUrl();
    }
</script>

<Tab>
    <ButtonSetting title="Link Account" on:click={addAccount}/>
    <ButtonSetting title="Copy URL" secondary={true} on:click={copyLoginUrl} inset={true}/>
</Tab>