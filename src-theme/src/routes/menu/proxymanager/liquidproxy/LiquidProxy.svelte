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
    import CircleLoader from "../../common/CircleLoader.svelte";
    import {notification} from "../../common/header/notification_store";
    import LocationMap from "./LocationMap.svelte";
    import PlanSelect from "./PlanSelect.svelte";
    import SessionHistory from "./SessionHistory.svelte";
    import SubscriptionModal from "./SubscriptionModal.svelte";
    import {formatDate, locationLabel} from "./liquidproxy";
    import LiquidProxyLogo from "./LiquidProxyLogo.svelte";
    import Welcome from "./Welcome.svelte";

    const SESSION_REFRESH_MS = 10_000;
    const LOCATION_REFRESH_MS = 30_000;
    // The client measures the ping to each location when asked for them; this picks up the results
    const PROBE_RESULT_DELAY_MS = 2_500;

    const dispatch = createEventDispatcher<{ switchView: void }>();

    let state: LiquidProxyState | null = null;
    let loadError: string | null = null;
    let locations: LiquidProxyLocation[] = [];
    let sessions: LiquidProxySession[] = [];
    let now = Date.now();

    let loggingIn = false;
    let subscriptionVisible = false;

    $: subscribed = state?.subscription?.state === "active";
    // Also when a subscription shows up while the screen is open
    $: if (subscribed) {
        refreshSessions();
    }

    const timers: ReturnType<typeof setInterval>[] = [];

    onMount(async () => {
        await Promise.all([loadState(true), loadLocations()]);
        await refreshSessions();

        timers.push(
            setTimeout(loadLocations, PROBE_RESULT_DELAY_MS),
            setInterval(loadLocations, LOCATION_REFRESH_MS),
            setInterval(() => {
                now = Date.now();
                refreshSessions();
            }, SESSION_REFRESH_MS)
        );
    });

    onDestroy(() => timers.forEach(clearTimeout));

    listen("userLoggedIn", () => loadState(true));
    listen("userLoggedOut", () => loadState());

    function notify(message: string, error = false) {
        notification.set({title: "LiquidProxy", message, error});
    }

    function notifyError(e: unknown) {
        notify((e as Error).message, true);
    }

    async function loadState(refresh = false) {
        try {
            state = await getLiquidProxyState(refresh);
            loadError = null;
        } catch (e) {
            loadError = (e as Error).message;
        }
    }

    async function loadLocations() {
        try {
            locations = await getLiquidProxyLocations();
        } catch {
            // The map stays empty; the state shows what went wrong
        }
    }

    async function refreshSessions() {
        if (!subscribed) {
            sessions = [];
            return;
        }
        try {
            sessions = await getLiquidProxySessions();
        } catch {
            // Keep the last list rather than flashing an empty one
        }
    }

    function label(code: string) {
        const location = locations.find(l => l.code === code);
        return location ? locationLabel(location, locations) : code;
    }

    async function login() {
        loggingIn = true;
        try {
            await loginClientUser();
            await loadState(true);
            await refreshSessions();
            notify(state?.email ? `Logged in as ${state.email}` : "Logged in");
        } catch (e) {
            notifyError(e);
        } finally {
            loggingIn = false;
        }
    }

    // The client opens the pending login again and answers once it is done, which login() already waits for
    function reopenLogin() {
        loginClientUser().catch(() => {});
    }

    async function logout() {
        await logoutClientUser();
        await loadState();
        sessions = [];
        notify("Logged out");
    }

    async function connect(code: string) {
        try {
            await connectToLiquidProxy(code);
            await loadState();
            notify(`Connected to ${label(code)}`);
        } catch (e) {
            notifyError(e);
        }
    }

    async function disconnect() {
        try {
            await disconnectFromLiquidProxy();
            await loadState();
            notify("Disconnected from LiquidProxy");
        } catch (e) {
            notifyError(e);
        }
    }

    async function changeLevel(level: number) {
        try {
            await setLiquidProxySettings({level});
            await loadState();
        } catch (e) {
            notifyError(e);
        }
    }

    async function changeForwardAuthentication(forwardAuthentication: boolean) {
        try {
            await setLiquidProxySettings({forwardAuthentication});
            await loadState();
        } catch (e) {
            notifyError(e);
        }
    }

    async function changeIp() {
        try {
            const {username, alreadyRequested} = await requestLiquidProxyNewIp();
            notify(alreadyRequested
                ? `${username} already gets a new IP on the next join`
                : `${username} gets a new IP on the next join`);
        } catch (e) {
            notifyError(e);
        }
    }

    async function endSession(id: string) {
        try {
            await endLiquidProxySession(id);
            notify("Session ended");
            await refreshSessions();
        } catch (e) {
            notifyError(e);
        }
    }
</script>

{#if state?.subscription}
    <SubscriptionModal bind:visible={subscriptionVisible} subscription={state.subscription} email={state.email}/>
{/if}

<div class="liquidproxy" transition:fly|global={{duration: 700, x: 1000}}>
    <div class="side">
        {#if state && subscribed}
            <div class="controls">
                <PlanSelect plans={state.plans} level={state.level} on:change={e => changeLevel(e.detail)}/>
                <SwitchSetting title="Forward Microsoft Authentication" value={state.forwardAuthentication}
                               on:change={() => changeForwardAuthentication(!state?.forwardAuthentication)}/>
            </div>
        {/if}

        <LocationMap {locations} connected={state?.connected} focus={state?.location} interactive={subscribed}
                     caption={subscribed ? null : "Global Coverage"}
                     on:connect={e => connect(e.detail)} on:disconnect={disconnect}/>
    </div>

    <div class="main">
        {#if state === null}
            <div class="panel">
                {#if loadError}
                    <LiquidProxyLogo/>
                    <div class="headline">LiquidProxy is not reachable</div>
                    <p>{loadError}</p>
                {:else}
                    <CircleLoader/>
                {/if}
            </div>
        {:else if !state.loggedIn}
            <Welcome locationCount={locations.length} {loggingIn}/>
        {:else if !subscribed}
            <div class="panel">
                <LiquidProxyLogo/>
                {#if state.subscription?.state === "unavailable"}
                    <div class="headline">Subscription unavailable</div>
                    <p>
                        Your LiquidProxy subscription can't be used right now.
                        Contact support if you think this is a mistake.
                    </p>
                {:else if state.subscription}
                    <div class="headline">Subscription ended</div>
                    <p>
                        Your LiquidProxy subscription ended on {formatDate(state.subscription.expiresAt)}.
                        Renew it to get back to playing.
                    </p>
                {:else}
                    <div class="headline">No subscription yet</div>
                    <p>
                        {state.email ?? "Your account"} has no LiquidProxy subscription.
                        Pick a plan on liquidproxy.net and it shows up here.
                    </p>
                {/if}
            </div>
        {:else}
            <SessionHistory {sessions} {locations} {now} on:end={e => endSession(e.detail)}/>
        {/if}
    </div>
</div>

<BottomButtonWrapper>
    <ButtonContainer>
        {#if state === null}
            <IconTextButton icon="icon-refresh.svg" title="Try Again" disabled={!loadError}
                            on:click={() => loadState(true)}/>
        {:else if !state.loggedIn}
            <IconTextButton icon="liquidproxy/log-in.svg" title={loggingIn ? "Open Login Again" : "Login"}
                            on:click={loggingIn ? reopenLogin : login}/>
            <IconTextButton icon="liquidproxy/tag.svg" title="View Plans" on:click={() => browse("PROXY_PLANS")}/>
        {:else}
            <IconTextButton icon="liquidproxy/log-out.svg" title="Logout" on:click={logout}/>
            {#if subscribed}
                <IconTextButton icon="icon-refresh.svg" title="Change IP on next join" on:click={changeIp}/>
                <IconTextButton icon="liquidproxy/credit-card.svg" title="Subscription Details"
                                on:click={() => subscriptionVisible = true}/>
            {:else}
                {#if state.subscription?.state === "unavailable"}
                    <IconTextButton icon="liquidproxy/life-buoy.svg" title="Contact Support"
                                    on:click={() => browse("PROXY_SUPPORT")}/>
                {:else if state.subscription}
                    <IconTextButton icon="liquidproxy/credit-card.svg" title="Renew"
                                    on:click={() => browse("PROXY_DASHBOARD")}/>
                {:else}
                    <IconTextButton icon="liquidproxy/tag.svg" title="View Plans"
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
