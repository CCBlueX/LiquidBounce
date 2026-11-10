<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import {fly} from "svelte/transition";
    import {
        changeLiquidProxySessionIp,
        connectToLiquidProxy,
        deleteScreen,
        disconnectFromLiquidProxy,
        endLiquidProxySession,
        getLiquidProxyLocations,
        getLiquidProxySessions,
        getLiquidProxyState,
        loginClientUser,
        logoutClientUser,
        openLink,
        setLiquidProxySettings,
    } from "../../../../integration/rest";
    import type {LiquidProxyLocation, LiquidProxySession, LiquidProxyState} from "../../../../integration/types";
    import type {NotificationEvent} from "../../../../integration/events";
    import {listen} from "../../../../integration/ws";
    import BottomButtonWrapper from "../../common/buttons/BottomButtonWrapper.svelte";
    import ButtonContainer from "../../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../../common/buttons/IconTextButton.svelte";
    import SwitchSetting from "../../common/setting/SwitchSetting.svelte";
    import SingleSelect from "../../common/setting/select/SingleSelect.svelte";
    import CircleLoader from "../../common/CircleLoader.svelte";
    import {notification} from "../../common/header/notification_store";
    import LocationMap from "./LocationMap.svelte";
    import SessionHistory from "./SessionHistory.svelte";
    import LiquidProxyLogo from "./LiquidProxyLogo.svelte";

    const dispatch = createEventDispatcher<{ switchView: void }>();

    let state: LiquidProxyState | null = null;
    let locations: LiquidProxyLocation[] = [];
    let sessions: LiquidProxySession[] = [];
    let now = Date.now();
    let loggingIn = false;

    $: active = state?.status === "ACTIVE";
    // Also when the subscription shows up or ends while the screen is open
    $: if (active) {
        loadSessions();
    } else {
        sessions = [];
    }

    onMount(() => {
        reload();

        const timers = [
            setInterval(loadLocations, 30_000),
            setInterval(() => {
                now = Date.now();
                if (active) {
                    loadSessions();
                }
            }, 10_000)
        ];
        return () => timers.forEach(clearInterval);
    });

    listen("userLoggedIn", () => loadState(true));
    listen("userLoggedOut", () => loadState());
    // The client says how a change went
    listen("notification", (e: NotificationEvent) => {
        notification.set({title: e.title, message: e.message, error: e.severity === "ERROR"});
    });

    function reload() {
        loadState(true);
        loadLocations();
    }

    // A request that fails keeps what is shown

    async function loadState(refresh = false) {
        state = await getLiquidProxyState(refresh) ?? state;
    }

    async function loadLocations() {
        locations = await getLiquidProxyLocations() ?? locations;
    }

    async function loadSessions() {
        const loaded = await getLiquidProxySessions();
        if (loaded) {
            sessions = loaded;
        } else {
            // The state says what is wrong
            loadState(true);
        }
    }

    async function changeState(change: Promise<LiquidProxyState | undefined>) {
        state = await change ?? state;
    }

    async function changeSessions(change: Promise<LiquidProxySession[] | undefined>) {
        sessions = await change ?? sessions;
    }

    function chooseNetwork(name: string) {
        const network = state?.networks.find(network => network.name === name);
        if (network) {
            changeState(setLiquidProxySettings({network: network.code}));
        }
    }

    // While a login is pending, the client opens it again and answers both requests once it is done
    async function login() {
        loggingIn = true;
        try {
            await loginClientUser();
        } finally {
            loggingIn = false;
        }
    }
</script>

<div class="liquidproxy" transition:fly|global={{duration: 700, x: 1000}}>
    <div class="side">
        {#if state && active}
            <div class="controls">
                <SingleSelect title="Type" options={state.networks.map(network => network.name)}
                              value={state.networks.find(network => network.code === state?.network)?.name ?? ""}
                              on:change={e => chooseNetwork(e.detail.value)}/>
                <SwitchSetting title="Forward Microsoft Authentication" value={state.forwardAuthentication}
                               on:change={() => changeState(setLiquidProxySettings({
                                   forwardAuthentication: !state?.forwardAuthentication
                               }))}/>
            </div>
        {/if}

        <LocationMap {locations} connected={state?.connected} focus={state?.location} interactive={active}
                     on:connect={e => changeState(connectToLiquidProxy(e.detail))}
                     on:disconnect={() => changeState(disconnectFromLiquidProxy())}/>
    </div>

    <div class="main">
        {#if state === null}
            <div class="panel">
                <CircleLoader/>
            </div>
        {:else if state.notice}
            <div class="panel">
                <LiquidProxyLogo/>
                <div class="headline">{state.notice.title}</div>
                <p>{state.notice.text}</p>
            </div>
        {:else}
            <SessionHistory {sessions} {now}
                            on:end={e => changeSessions(endLiquidProxySession(e.detail))}
                            on:changeIp={e => changeSessions(changeLiquidProxySessionIp(e.detail))}/>
        {/if}
    </div>
</div>

<BottomButtonWrapper>
    <ButtonContainer>
        {#if state === null}
            <!-- Loading -->
        {:else if state.status === "LOGGED_OUT"}
            <IconTextButton icon="icon-user.svg" title={loggingIn ? "Open Login Again" : "Login"} on:click={login}/>
            <IconTextButton icon="icon-liquidbounce.net.svg" title="View Plans"
                            on:click={() => openLink("PROXY_PLANS")}/>
        {:else}
            <IconTextButton icon="icon-exit.svg" title="Logout" on:click={logoutClientUser}/>
            {#if state.status === "NO_SUBSCRIPTION"}
                <IconTextButton icon="icon-liquidbounce.net.svg" title="View Plans"
                                on:click={() => openLink("PROXY_PLANS")}/>
            {:else if state.status === "EXPIRED"}
                <IconTextButton icon="icon-liquidbounce.net.svg" title="Renew"
                                on:click={() => openLink("PROXY_SUBSCRIPTION")}/>
            {:else if state.status === "UNAVAILABLE"}
                <IconTextButton icon="icon-liquidbounce.net.svg" title="Contact Support"
                                on:click={() => openLink("PROXY_SUPPORT")}/>
            {/if}
            {#if !active}
                <IconTextButton icon="icon-refresh.svg"
                                title={state.status === "UNREACHABLE" ? "Try Again" : "Check Again"}
                                on:click={reload}/>
            {/if}
        {/if}
    </ButtonContainer>

    <ButtonContainer>
        <IconTextButton icon="icon-proxymanager.svg" title="Custom Proxies" on:click={() => dispatch("switchView")}/>
        <IconTextButton icon="icon-back.svg" title="Back" on:click={() => deleteScreen()}/>
    </ButtonContainer>
</BottomButtonWrapper>

<style lang="scss">
  .liquidproxy {
    // A definite basis, so that a long session list scrolls instead of growing the panel
    flex: 1 1 0px;
    display: grid;
    grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
    column-gap: 40px;
    min-height: 0;
    margin-bottom: 25px;
    padding: 35px;
    border-radius: 5px;
    background-color: var(--menu-button-container-background-color);
  }

  .side {
    display: flex;
    flex-direction: column;
    row-gap: 20px;
    min-height: 0;

    :global(.map) {
      flex: 1;
    }
  }

  .controls {
    display: flex;
    align-items: center;
    column-gap: 40px;
    color: var(--menu-text-color);
    font-size: 20px;
    font-weight: 500;
    // The plan dropdown opens over the map
    position: relative;
    z-index: 10;
  }

  .main {
    display: flex;
    flex-direction: column;
    min-height: 0;

    > :global(.session-history) {
      flex: 1;
    }
  }

  .panel {
    display: flex;
    flex-direction: column;
    justify-content: center;
    row-gap: 25px;
    flex: 1;
    max-width: 640px;
    color: var(--menu-text-dimmed-color);
    font-size: 20px;
    line-height: 1.4;

    p {
      margin: 0;
    }
  }

  .headline {
    color: var(--menu-text-color);
    font-size: 34px;
    font-weight: 600;
  }

</style>
