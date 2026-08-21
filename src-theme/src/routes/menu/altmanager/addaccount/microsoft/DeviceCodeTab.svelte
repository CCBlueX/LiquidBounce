<script lang="ts">
    import ButtonSetting from "../../../common/setting/ButtonSetting.svelte";
    import Tab from "../../../common/modal/Tab.svelte";
    import {addMicrosoftAccountDeviceCode, addMicrosoftAccountDeviceCodeCopyUrl} from "../../../../../integration/rest";

    let loading = false;

    async function addAccount() {
        loading = true;
        await addMicrosoftAccountDeviceCode();
    }

    async function copyUrl() {
        loading = true;
        await addMicrosoftAccountDeviceCodeCopyUrl();
    }
</script>

<Tab>
    <ButtonSetting title="Link Account" on:click={addAccount} {loading}/>
    <ButtonSetting title="Copy URL" on:click={copyUrl} secondary={true} />
</Tab>
