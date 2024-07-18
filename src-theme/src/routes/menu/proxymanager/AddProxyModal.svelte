<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import {addProxy as addProxyRest} from "../../../integration/rest";

    export let visible: boolean;

    $: disabled = validateInput(requiresAuthentication, hostPort, username, password);
    $: {
        if (!requiresAuthentication) {
            username = "";
            password = "";
        }
    }

    let requiresAuthentication = false;
    let hostPort = "";
    let username = "";
    let password = "";

    function validateInput(requiresAuthentication: boolean, host: string, username: string, password: string): boolean {
        let valid = /.+:[0-9]+/.test(host);

        if (requiresAuthentication) {
            valid &&= username.length > 0 && password.length > 0;
        }

        return !valid;
    }

    async function addProxy() {
        if (disabled) {
            return;
        }
        const [host, port] = hostPort.split(":");

        await addProxyRest(host, parseInt(port), username, password);
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

<Modal title="Add Proxy" bind:visible={visible} on:close={cleanup}>
    <IconTextInput title="Host:Port" icon="server" pattern=".+:[0-9]+" bind:value={hostPort}/>
    <SwitchSetting title="Requires Authentication" bind:value={requiresAuthentication}/>
    {#if requiresAuthentication}
        <IconTextInput title="Username" icon="user" bind:value={username}/>
        <IconTextInput title="Password" icon="lock" type="password" bind:value={password}/>
    {/if}
    <ButtonSetting title="Add Proxy" {disabled} on:click={addProxy} listenForEnter={true}/>
</Modal>
