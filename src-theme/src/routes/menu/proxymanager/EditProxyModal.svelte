<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import type {Proxy} from "../../../integration/types";
    import {editProxy as editProxyRest} from "../../../integration/rest";

    export let visible: boolean;
    export let selectedProxy: Proxy;
    export let hostPort = "";
    export let username = "";
    export let password = "";
    export let requiresAuthentication = false;

    $: disabled = validateInput(requiresAuthentication, hostPort, username, password);

    function validateInput(requiresAuthentication: boolean, host: string, username: string, password: string): boolean {
        let valid = /.+:[0-9]+/.test(host);

        if (requiresAuthentication) {
            valid &&= username.length > 0 && password.length > 0;
        }

        return !valid;
    }

    async function editProxy() {
        if (disabled) {
            return;
        }

        const [host, port] = hostPort.split(":");

        await editProxyRest(selectedProxy.id, host, parseInt(port), username, password);
        visible = false;
        cleanup();
    }

    function cleanup() {
        requiresAuthentication = false;
        hostPort = "";
        username = "";
        password = "";
    }
</script>

<Modal title="Edit Proxy" bind:visible={visible} on:close={cleanup}>
    <IconTextInput title="Host:Port" icon="server" pattern=".+:[0-9]+" bind:value={hostPort}/>
    <SwitchSetting title="Requires Authentication" bind:value={requiresAuthentication}/>
    {#if requiresAuthentication}
        <IconTextInput title="Username" icon="user" bind:value={username}/>
        <IconTextInput title="Password" icon="lock" type="password" bind:value={password}/>
    {/if}
    <ButtonSetting title="Edit Proxy" {disabled} on:click={editProxy} listenForEnter={true}/>
</Modal>
