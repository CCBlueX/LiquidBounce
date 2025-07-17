<script lang="ts">
    import Modal from "../common/modal/Modal.svelte";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import {editProxy as editProxyRest} from "../../../integration/rest";
    import {afterUpdate} from "svelte";
    import {listen} from "../../../integration/ws";
    import SingleSelect from "../common/setting/select/SingleSelect.svelte";

    export let visible: boolean;
    export let id: number;
    export let proxyType: string;
    export let host: string;
    export let port: number;
    export let username: string;
    export let password: string;
    export let requiresAuthentication: boolean;
    export let forwardAuthentication: boolean;

    let hostPort = `${host}:${port}`;
    let loading = false;

    $: disabled = validateInput(requiresAuthentication, hostPort, username, password);
    $: {
        if (!requiresAuthentication) {
            username = "";
            password = "";
        }
    }

    function validateInput(requiresAuthentication: boolean, hostPort: string, username: string, password: string): boolean {
        let valid = /.+:[0-9]+/.test(hostPort);

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

        loading = true;
        await editProxyRest(id, host, parseInt(port), username, password, proxyType, forwardAuthentication);
    }

    listen("proxyEditResult", () => {
        visible = false;
        loading = false;
    });
</script>

<Modal title="Edit Proxy" bind:visible={visible}>
    <IconTextInput title="Host:Port" icon="server" pattern=".+:[0-9]+" bind:value={hostPort}/>
    <SingleSelect title="Proxy Type" options={["HTTP", "SOCKS5"]} bind:value={proxyType}/>
    <SwitchSetting title="Requires Authentication" bind:value={requiresAuthentication}/>
    {#if requiresAuthentication}
        <IconTextInput title="Username" icon="user" bind:value={username}/>
        <IconTextInput title="Password" icon="lock" type="password" bind:value={password}/>
    {/if}
    <SwitchSetting title="Forward Microsoft Authentication" bind:value={forwardAuthentication}/>
    <ButtonSetting title="Edit Proxy" {disabled} on:click={editProxy} listenForEnter={true} {loading}/>
</Modal>
