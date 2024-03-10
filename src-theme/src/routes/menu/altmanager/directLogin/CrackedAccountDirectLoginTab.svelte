<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {directLoginToCrackedAccount} from "../../../../integration/rest";

    let username = "";
    $: disabled = validateUsername(username);

    async function login() {
        await directLoginToCrackedAccount(username);
    }

    function validateUsername(username: string): boolean {
        return !/^[a-zA-Z0-9_]{1,16}$/.test(username);
    }
</script>

<Tab>
    <IconTextInput icon="user" title="Username" pattern={"[a-zA-Z0-9_]{1,16}"} bind:value={username} maxLength={16}/>
    <ButtonSetting {disabled} title="Login" on:click={login} inset={true}/>
</Tab>