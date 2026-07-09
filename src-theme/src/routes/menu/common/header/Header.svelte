<script lang="ts">
    import LiquidBounceLogo from "../../../../components/LiquidBounceLogo.svelte";
    import Account from "./account/Account.svelte";
    import AnimatedLogo from "./AnimatedLogo.svelte";
    import Notifications from "./Notifications.svelte";
    import {listen} from "../../../../integration/ws";
    import {location} from "svelte-spa-router";
    import type {
        AccountManagerAdditionEvent,
        AccountManagerLoginEvent,
        AccountManagerMessageEvent
    } from "../../../../integration/events";
    import {notification} from "./notification_store";
    import {isAnniversary} from "../../../../util/utils";

    $: showAnniversaryLogo = $location === "/title" && isAnniversary();

    listen("accountManagerAddition", (e: AccountManagerAdditionEvent) => {
        if (!e.error) {
            notification.set({
                title: "AltManager",
                message: `Successfully added account ${e.username}`,
                error: false
            });
        } else {
            notification.set({
                title: "AltManager",
                message: e.error,
                error: true
            });
        }
    });

    listen("accountManagerMessage", (e: AccountManagerMessageEvent) => {
        notification.set({
            title: "AltManager",
            message: e.message,
            error: false
        });
    });

    listen("accountManagerLogin", (e: AccountManagerLoginEvent) => {
        if (!e.error) {
            notification.set({
                title: "AltManager",
                message: `Successfully logged in to account ${e.username}`,
                error: false
            });
        } else {
            notification.set({
                title: "AltManager",
                message: e.error,
                error: true
            });
        }
    });
</script>

<div class="header">
    <div class="logo-wrapper">
        <div class="logo" class:visible={showAnniversaryLogo} aria-hidden={!showAnniversaryLogo}>
            <AnimatedLogo/>
        </div>
        <div class="logo" class:visible={!showAnniversaryLogo} aria-hidden={showAnniversaryLogo}>
            <LiquidBounceLogo
                    width="261.263px"
                    height="98px"
                    badgeFill="var(--accent-color)"
            />
        </div>
    </div>

    <Notifications/>

    <Account/>
</div>

<style lang="scss">
  .header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 60px;
    align-items: center;
  }

  .logo-wrapper {
    display: grid;
  }

  .logo {
    grid-area: 1 / 1;
    opacity: 0;
    pointer-events: none;
    transition: opacity .5s ease;

    &.visible {
      opacity: 1;
    }
  }
</style>
