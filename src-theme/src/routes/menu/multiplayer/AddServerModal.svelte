<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import SingleSelect from "../common/setting/select/SingleSelect.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import {addServer as restAddServer} from "../../../integration/rest";
    import {createEventDispatcher} from "svelte";

    export let visible: boolean;

    const dispatch = createEventDispatcher();

    let name = "";
    let address = "";
    let serverResourcePacks = "Prompt";

    async function addServer() {
        if (!address || !name) {
            return;
        }

        await restAddServer(name, address, serverResourcePacks);
        dispatch("serverAdd");
        cleanUp();
        visible = false;
    }

    function cleanUp() {
        name = "";
        address = "";
        serverResourcePacks = "";
    }
</script>

<Modal bind:visible={visible} title="Add Server" on:close={cleanUp}>
    <IconTextInput title="Name" icon="exit" bind:value={name}/>
    <IconTextInput title="Address" icon="exit" bind:value={address}/>
    <SingleSelect title="Server Resource Packs" options={["Prompt", "Enabled", "Disabled"]} bind:value={serverResourcePacks}/>
    <ButtonSetting title="Add Server" on:click={addServer}/>
</Modal>