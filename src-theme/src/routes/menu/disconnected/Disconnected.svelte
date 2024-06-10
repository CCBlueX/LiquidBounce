<script lang="ts">
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import {
        directLoginToCrackedAccount,
        getAccounts,
        directLoginToClip,
        reconnectToServer
    } from "../../../integration/rest";
    import type {AccountManagerLoginEvent} from "../../../integration/events";
    import IconTextInput from "../common/setting/IconTextInput.svelte";
    import IconButton from "../common/buttons/IconButton.svelte";
    import {listen} from "../../../integration/ws";
    import {onMount} from "svelte";
    import type {Account} from "../../../integration/types";

    let premiumAccounts: Account[] = [];
    let username = "";
    $: disabled = validateUsername(username);

    function validateUsername(username: string): boolean {
        return !/^[a-zA-Z0-9_]{1,16}$/.test(username);
    }

    function generateRandomUsername() {
        const n = Math.floor(Math.random() * 1e6) + 1
        username = ("bool"+n).substring(0, 16).replace(/[^a-zA-Z0-9_]+/gi, "");
    }

    async function reconnectWithRandomUsername() {
        generateRandomUsername()
        await directLoginToCrackedAccount(username,false);
    }

    async function loginToUserFromTxtBox() {
        await directLoginToCrackedAccount(username,true);
    }

    onMount(async () => {
        premiumAccounts = (await getAccounts()).filter(a => a.type !== "Cracked" && !a.favorite);

        setTimeout(() => { // TODO: Hacky fix for issues caused by stuck route fix
            listen("accountManagerLogin", async (e: AccountManagerLoginEvent) => {
                await reconnectToServer();
            });
        }, 1000);
    });
</script>

<div class="reconnect">
    <ButtonSetting title="Reconnect" on:click={() => reconnectToServer()}/>
    <ButtonSetting title="Reconnect with clipboard" on:click={directLoginToClip}/>
    <ButtonSetting title="Reconnect with random username" on:click={reconnectWithRandomUsername}/>
</div>

<div class="cumcakes">
    <IconTextInput icon="user" title="Username" pattern={"[a-zA-Z0-9_]{1,16}"} bind:value={username} maxLength={16}>
        <IconButton icon="random" title="Random" on:click={generateRandomUsername}/>
    </IconTextInput>
    <ButtonSetting {disabled} title="Use Account" on:click={loginToUserFromTxtBox} inset={true}/>
</div>

<style lang="scss">
    .reconnect {
        position: fixed;
        bottom: 20px;
        left: 45px;
        display: flex;
        flex-direction: column;
        row-gap: 10px;
        align-items: flex-start;
    }
    .cumcakes {
        position: fixed;
        bottom: 20px;
        right: 45px;
        display: flex;
        flex-direction: column;
        row-gap: 10px;
        align-items: flex-start;
    }
</style>
