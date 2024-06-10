<script lang="ts">
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import {
        directLoginToCrackedAccount,
        getAccounts,
        directLoginToClip,
        reconnectToServer
    } from "../../../integration/rest";
    import type {AccountManagerLoginEvent} from "../../../integration/events";
    import {listen} from "../../../integration/ws";
    import {onMount} from "svelte";
    import type {Account} from "../../../integration/types";

    let premiumAccounts: Account[] = [];

    async function reconnectWithRandomUsername() {
        const n = Math.floor(Math.random() * 1e6) + 1
        const username = ("bool"+n).substring(0, 16).replace(/[^a-zA-Z0-9_]+/gi, "");
        await directLoginToCrackedAccount(username,false);
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
</style>
