<script lang="ts">
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import {fly} from "svelte/transition";
    import {
        browse,
        connectToLiquidProxy,
        deleteScreen,
        disconnectFromLiquidProxy,
        endLiquidProxySession,
        getLiquidProxyLocations,
        getLiquidProxySessions,
        getLiquidProxyState,
        loginClientUser,
        logoutClientUser,
        requestLiquidProxyNewIp,
        setLiquidProxySettings,
    } from "../../../../integration/rest";
    import type {LiquidProxyLocation, LiquidProxySession, LiquidProxyState} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import BottomButtonWrapper from "../../common/buttons/BottomButtonWrapper.svelte";
    import ButtonContainer from "../../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../../common/buttons/IconTextButton.svelte";
    import SwitchSetting from "../../common/setting/SwitchSetting.svelte";
    import SingleSelect from "../../common/setting/select/SingleSelect.svelte";
    import CircleLoader from "../../common/CircleLoader.svelte";
    import LocationMap from "./LocationMap.svelte";
    import SessionHistory from "./SessionHistory.svelte";
    import LiquidProxyLogo from "./LiquidProxyLogo.svelte";

    const dispatch = createEventDispatcher<{ switchView: void }>();

    let state: LiquidProxyState | null = null;
    let locations: LiquidProxyLocation[] = [];
    let sessions: LiquidProxySession[] = [];
    let now = Date.now();

    let loggingIn = false;

    $: subscribed = state?.subscription === "active";
    // Also when a subscription shows up while the screen is open
    $: if (subscribed) {
        refreshSessions();
    }

    const timers: ReturnType<typeof setInterval>[] = [];

    onMount(async () => {
        await Promise.all([loadState(true), loadLocations()]);

        timers.push(
            // The client measures the ping to each location when asked for them; this picks up the results
            setTimeout(loadLocations, 2_500),
            setInterval(loadLocations, 30_000),
            setInterval(() => {
                now = Date.now();
                refreshSessions();
            }, 10_000)
        );
    });

    onDestroy(() => timers.forEach(clearTimeout));

    listen("userLoggedIn", () => loadState(true));
    listen("userLoggedOut", () => loadState());

    async function loadState(refresh = false) {
        state = await getLiquidProxyState(refresh);
    }

    async function loadLocations() {
        locations = await getLiquidProxyLocations();
    }

    async function refreshSessions() {
        // Keeps the last list when the client can't get a new one, rather than flashing an empty one
        sessions = subscribed ? await getLiquidProxySessions() ?? sessions : [];
    }

    async function login() {
        loggingIn = true;
        try {
            await loginClientUser();
            await loadState(true);
        } finally {
            loggingIn = false;
        }
    }

    // The client opens the pending login again and answers once it is done, which login() already waits for
    function reopenLogin() {
        loginClientUser();
    }

    async function logout() {
        await logoutClientUser();
        await loadState();
        sessions = [];
    }

    async function endSession(id: string) {
        await endLiquidProxySession(id);
        await refreshSessions();
    }

    async function choosePlan(name: string) {
        const plan = state?.plans.find(plan => plan.name === name);
        if (plan) {
            state = await setLiquidProxySettings({level: plan.level});
        }
    }
</script>

<div class="liquidproxy" transition:fly|global={{duration: 700, x: 1000}}>
    <div class="side">
        {#if state && subscribed}
            <div class="controls">
                <SingleSelect title="Type" options={state.plans.map(plan => plan.name)}
                              value={state.plans.find(plan => plan.level === state?.level)?.name ?? ""}
                              on:change={e => choosePlan(e.detail.value)}/>
                <SwitchSetting title="Forward Microsoft Authentication" value={state.forwardAuthentication}
                               on:change={async () => state = await setLiquidProxySettings({
                                   forwardAuthentication: !state?.forwardAuthentication
                               })}/>
            </div>
        {/if}

        <LocationMap {locations} connected={state?.connected} focus={state?.location} interactive={subscribed}
                     on:connect={async e => state = await connectToLiquidProxy(e.detail)}
                     on:disconnect={async () => state = await disconnectFromLiquidProxy()}/>
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
            <SessionHistory {sessions} {now} on:end={e => endSession(e.detail)}/>
        {/if}
    </div>
</div>

<BottomButtonWrapper>
    <ButtonContainer>
        {#if state === null}
            <!-- Loading -->
        {:else if !state.reachable}
            <IconTextButton icon="icon-refresh.svg" title="Try Again" on:click={() => loadState(true)}/>
        {:else if !state.loggedIn}
            <IconTextButton icon="icon-user.svg" title={loggingIn ? "Open Login Again" : "Login"}
                            on:click={loggingIn ? reopenLogin : login}/>
            <IconTextButton icon="icon-liquidbounce.net.svg" title="View Plans"
                            on:click={() => browse("PROXY_PLANS")}/>
        {:else}
            <IconTextButton icon="icon-exit.svg" title="Logout" on:click={logout}/>
            {#if subscribed}
                <IconTextButton icon="icon-refresh.svg" title="Change IP on next join"
                                on:click={requestLiquidProxyNewIp}/>
            {:else}
                {#if state.subscription === "unavailable"}
                    <IconTextButton icon="icon-liquidbounce.net.svg" title="Contact Support"
                                    on:click={() => browse("PROXY_SUPPORT")}/>
                {:else if state.subscription}
                    <IconTextButton icon="icon-liquidbounce.net.svg" title="Renew"
                                    on:click={() => browse("PROXY_DASHBOARD")}/>
                {:else}
                    <IconTextButton icon="icon-liquidbounce.net.svg" title="View Plans"
                                    on:click={() => browse("PROXY_PLANS")}/>
                {/if}
                <IconTextButton icon="icon-refresh.svg" title="Check Again" on:click={() => loadState(true)}/>
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
    flex: 1;
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
