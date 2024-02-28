<script lang="ts">
    import Modal from "../common/Modal.svelte";
    import SingleSelect from "../common/setting/SingleSelect.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import {editServer as editServerRest} from "../../../integration/rest";
    import {createEventDispatcher} from "svelte";

    export let visible: boolean;
    export let name: string;
    export let address: string;
    export let serverResourcePacks: string;
    export let index: number;

    const dispatch = createEventDispatcher();

    async function editServer() {
        if (!address || !name) {
            return;
        }

        await editServerRest(index, name, address, serverResourcePacks);
        dispatch("serverEdit");
        visible = false;
    }
</script>

<Modal bind:visible={visible} title="Edit Server">
    <IconTextInput title="Name" icon="exit" bind:value={name}/>
    <IconTextInput title="Address" icon="exit" bind:value={address}/>
    <SingleSelect title="Server Resource Packs" options={["Prompt", "Enabled", "Disabled"]} bind:value={serverResourcePacks}/>
    <ButtonSetting title="Edit Server" on:click={editServer}/>
</Modal>