<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {directLoginToCrackedAccount} from "../../../../integration/rest";
    import IconButton from "../../common/buttons/IconButton.svelte";
    import {faker} from "@faker-js/faker";
    import SwitchSetting from "../../common/setting/SwitchSetting.svelte";

    let username = "";
    let online = false;
    $: disabled = validateUsername(username);

    async function login() {
        if (disabled) {
            return;
        }
        await directLoginToCrackedAccount(username, online);
    }

    function validateUsername(username: string): boolean {
        return !/^[a-zA-Z0-9_]{1,16}$/.test(username);
    }

    function generateRandomUsername() {
        username = faker.internet.userName().substring(0, 16).replace(/[^a-zA-Z0-9_]+/gi, "");
    }
</script>

<Tab>
    <IconTextInput icon="user" title="Username" pattern={"[a-zA-Z0-9_]{1,16}"} bind:value={username} maxLength={16}>
        <IconButton icon="random" title="Random" on:click={generateRandomUsername}/>
    </IconTextInput>
    <SwitchSetting title="Use online UUID" bind:value={online}/>
    <ButtonSetting {disabled} title="Login" on:click={login} listenForEnter={true} inset={true}/>
</Tab>