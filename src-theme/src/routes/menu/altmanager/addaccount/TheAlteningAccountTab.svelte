<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {createEventDispatcher} from "svelte";
    import {addAlteningAccount} from "../../../../integration/rest";

    let token = "";
    $: disabled = validateToken(token);

    const dispatch = createEventDispatcher();

    function validateToken(token: string) {
        return token.length === 0;
    }

    async function addAccount() {
        await addAlteningAccount(token);
        dispatch("modify");
    }
</script>

<Tab>
    <IconTextInput icon="exit" title="Token" bind:value={token}/>
    <ButtonSetting {disabled} title="Add Account" on:click={addAccount} />
</Tab>