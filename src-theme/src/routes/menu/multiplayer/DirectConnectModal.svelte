<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import {connectToServer} from "../../../integration/rest";
    import {setItem} from "../../../integration/persistent_storage";

    export let visible: boolean;

    let address = localStorage.getItem("multiplayer_direct_connect_address") ?? "";

    $: disabled = validateInput(address);

    function validateInput(address: string): boolean {
        return address.length === 0;
    }

    async function connect() {
        if (disabled) {
            return;
        }
        visible = false;
        await setItem("multiplayer_direct_connect_address", address)
        await connectToServer(address);
    }
</script>

<Modal bind:visible={visible} title="Direct Connection">
    <IconTextInput title="Address" icon="server" bind:value={address}/>
    <ButtonSetting title="Join Server" on:click={connect} {disabled} listenForEnter={true} inset={true}/>
</Modal>