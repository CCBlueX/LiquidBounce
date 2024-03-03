<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import {connectToServer} from "../../../integration/rest";

    export let visible: boolean;

    let address = localStorage.getItem("multiplayer_direct_connect_address") ?? "";

    $: disabled = validateInput(address);

    function validateInput(address: string): boolean {
        return address.length === 0;
    }

    async function addServer() {
        visible = false;
        localStorage.setItem("multiplayer_direct_connect_address", address)
        await connectToServer(address);
    }

    function cleanUp() {
        address = "";
    }
</script>

<Modal bind:visible={visible} title="Direct Connection" on:close={cleanUp}>
    <IconTextInput title="Address" icon="exit" bind:value={address}/>
    <ButtonSetting title="Join Server" on:click={addServer} {disabled}/>
</Modal>