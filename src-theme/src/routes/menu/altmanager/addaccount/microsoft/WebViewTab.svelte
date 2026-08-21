<script lang="ts">
    import ButtonSetting from "../../../common/setting/ButtonSetting.svelte";
    import Tab from "../../../common/modal/Tab.svelte";
    import {addMicrosoftAccountWebView} from "../../../../../integration/rest";

    let loading = false;

    async function addAccount() {
        loading = true;
        await addMicrosoftAccountWebView();
    }
</script>

<Tab>
    <ButtonSetting title="Sign in with web view" on:click={addAccount} {loading}/>
</Tab>
