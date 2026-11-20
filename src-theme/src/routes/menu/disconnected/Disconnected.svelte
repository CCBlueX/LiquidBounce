<script lang="ts">
    import Account from "../common/header/account/Account.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import {AutoReconnect} from "./autoReconnect.svelte";
    import {listen} from "../../../integration/ws";
    import {isLoggingIn} from "../altmanager/altmanager_store";

    const auto = new AutoReconnect();
    let accountHovered = $state(false);

    $effect(() => auto.run($isLoggingIn || accountHovered));

    listen("accountManagerLogin", () => auto.reconnect());

    const reconnectTitle = $derived(
        auto.reconnecting ? "Reconnecting..." : auto.enabled ? `Reconnecting in ${auto.secondsLeft}s` : "Reconnect"
    );
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="account" onmouseenter={() => accountHovered = true} onmouseleave={() => accountHovered = false}>
    <Account/>
</div>

<div class="reconnect">
    <ButtonSetting title={reconnectTitle} disabled={$isLoggingIn} on:click={() => auto.reconnect()}/>
    <SwitchSetting title="Auto reconnect" value={auto.enabled} on:change={() => auto.toggle()}/>
</div>

<style lang="scss">
  .account {
    position: fixed;
    top: 50px;
    right: 50px;
  }

  .reconnect {
    position: fixed;
    bottom: 20px;
    left: 45px;
    display: flex;
    align-items: center;
    column-gap: 20px;
  }

  @media screen and (max-height: 1100px) {
    .account {
      zoom: 0.8;
    }
  }

  @media screen and (max-height: 700px) {
    .account {
      zoom: 0.5;
    }
  }
</style>
