<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import SingleSelect from "../common/setting/select/SingleSelect.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import {addServer as restAddServer} from "../../../integration/rest";
    import {createEventDispatcher} from "svelte";

    export let visible: boolean;

    const dispatch = createEventDispatcher();

    let name = "Minecraft Server";
    let address = "";
    let resourcePackPolicy = "Prompt";

    $: disabled = validateInput(address, name);
    $: address = address.trim();

    function validateInput(address: string, name: string): boolean {
        return address.length === 0 || name.length === 0;
    }

    async function addServer() {
        if (disabled) {
            return;
        }
        await restAddServer(name, address, resourcePackPolicy);
        dispatch("serverAdd");
        cleanUp();
        visible = false;
    }

    function cleanUp() {
        name = "Minecraft Server";
        address = "";
        resourcePackPolicy = "";
    }
</script>

<Modal bind:visible={visible} title="Add Server" on:close={cleanUp}>
    <IconTextInput title="Name" icon="info" bind:value={name}/>
    <IconTextInput title="Address" icon="server" bind:value={address}/>
    <SingleSelect title="Server Resource Packs" options={["Prompt", "Enabled", "Disabled"]} bind:value={resourcePackPolicy}/>
    <ButtonSetting title="Add Server" on:click={addServer} {disabled} listenForEnter={true} inset={true}/>
</Modal>
