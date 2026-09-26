<script lang="ts">
    import Modal from "../../common/modal/Modal.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {browse, copyLiquidProxyCredentials, getLiquidProxyCredentials} from "../../../../integration/rest";
    import type {LiquidProxyCredentials, LiquidProxySubscription} from "../../../../integration/types";
    import {notification} from "../../common/header/notification_store";
    import {formatDate, subscriptionStatus} from "./liquidproxy";

    export let visible: boolean;
    export let subscription: LiquidProxySubscription;
    export let email: string | undefined;

    let credentials: LiquidProxyCredentials | null = null;
    let revealed = false;

    $: if (visible) {
        revealed = false;
        loadCredentials();
    }

    async function loadCredentials() {
        try {
            credentials = await getLiquidProxyCredentials();
        } catch {
            credentials = null;
        }
    }

    async function copy() {
        try {
            await copyLiquidProxyCredentials();
            notification.set({title: "LiquidProxy", message: "Copied the proxy", error: false});
        } catch (e) {
            notification.set({title: "LiquidProxy", message: (e as Error).message, error: true});
        }
    }
</script>

<Modal title="Subscription" bind:visible>
    <div class="details">
        <span class="label">Plan</span>
        <span>{subscription.plan}</span>

        <span class="label">Status</span>
        <span class:active={subscription.state === "active"}>{subscriptionStatus(subscription)}</span>

        <span class="label">{subscription.autoRenew ? "Renews" : "Ends"}</span>
        <span>{formatDate(subscription.expiresAt)}</span>

        {#if email}
            <span class="label">Account</span>
            <span>{email}</span>
        {/if}
    </div>

    {#if credentials}
        <div class="credentials">
            <span class="label">Use it outside LiquidBounce</span>
            <div class="proxy">
                <span class="text">
                    {credentials.host}:{credentials.port}:{credentials.username}:{revealed ? credentials.password : "••••••••"}
                </span>
                <button on:click={() => revealed = !revealed}>
                    <img src="img/menu/icon-eye.svg" alt="show">
                </button>
                <button on:click={copy}>
                    <img src="img/menu/icon-clipboard.svg" alt="copy">
                </button>
            </div>
        </div>
    {/if}

    <ButtonSetting title="Manage on liquidproxy.net" on:click={() => browse("PROXY_DASHBOARD")}/>
</Modal>

<style lang="scss">
  .details {
    display: grid;
    grid-template-columns: max-content 1fr;
    column-gap: 40px;
    row-gap: 12px;
    color: var(--menu-text-color);
    font-size: 18px;

    .active {
      color: var(--success-color);
    }
  }

  .label {
    color: var(--menu-text-dimmed-color);
  }

  .credentials {
    display: flex;
    flex-direction: column;
    row-gap: 10px;
    font-size: 18px;
  }

  .proxy {
    display: flex;
    align-items: center;
    column-gap: 15px;
    background-color: var(--menu-input-background-color, var(--menu-base-36-color));
    border-radius: 5px;
    padding: 12px 20px;

    .text {
      flex: 1;
      color: var(--menu-text-color);
      font-size: 16px;
      font-family: monospace;
      word-break: break-all;
    }

    button {
      display: flex;
      background: none;
      border: none;
      cursor: pointer;
      padding: 0;

      img {
        height: 20px;
      }
    }
  }
</style>
