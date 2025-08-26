<script lang="ts">
    import {onMount, tick} from 'svelte';
    import {fade, fly, slide} from 'svelte/transition';
    import {createEventDispatcher} from 'svelte';
    import {currentLogo} from '../common/header/logoStorage'
    import type {TransitionConfig} from 'svelte/transition'
    import {get, writable} from 'svelte/store';
    import {locked, unlock, shouldZoom} from './locked_store';
    import LoginMenu from './LoginMenu.svelte';
    import {getShaderEnabled, openScreen, setShaderEnabled} from "../../../integration/rest";
    import Background from "../common/Background.svelte";
    import {userSettings} from './userSettings';
    import type {KeyboardKeyEvent} from "../../../integration/events";
    import {listen} from "../../../integration/ws";

    enum UserStatus {
        LoggedOut = "Logged Out",
        LoggingIn = "Logging In",
        VerifyingLogIn = "Verifying Log In",
        LogInError = "Log In Error",
        LoggedIn = "Logged In",
        SetupRequired = "Setup Required"
    }

    const initialLocked = get(locked);
    export const userStatus = writable<UserStatus>(
        initialLocked
            ? UserStatus.LoggedOut
            : UserStatus.LoggedIn
    );
    let uidVisible: boolean[] = [false, false, false, false];
    let inputLocked = false;


    function handleKeydown(event: KeyboardKeyEvent) {
        const currentStatus = get(userStatus);

        if (currentStatus === UserStatus.SetupRequired || currentStatus === UserStatus.VerifyingLogIn) {
            return;
        }

        if (
            event.key !== "Escape" &&
            !event.key.match(/^F[1-9]$|^F1[0-2]$/) &&
            currentStatus === UserStatus.LoggedOut
        ) {
            startLogin();

        } else if (event.key === "Escape" && currentStatus === UserStatus.LoggingIn) {
            cancelLogin();
        }
    }

    $: {
        const shouldZoomNow = ![UserStatus.LoggedOut, UserStatus.SetupRequired].includes($userStatus);
        if ($shouldZoom !== shouldZoomNow) {
            $shouldZoom = shouldZoomNow;
        }
    }

    function slideReverse(node: Element, options: any): TransitionConfig {
        return slide(node, {...options, x: -100});
    }

    let uid = "";
    let hiddenInput: HTMLInputElement;
    let showError = false;
    const dispatch = createEventDispatcher();

    $: if ((get(userStatus) === UserStatus.LoggingIn || get(userStatus) === UserStatus.LogInError) && hiddenInput) {
        tick().then(() => hiddenInput.focus());
    }

    function formatTime(date: Date): string {
        const hours = date.getHours() % 12 || 12;
        const minutes = date.getMinutes().toString().padStart(2, '0');
        return `${hours}:${minutes}`;
    }

    function handlePinChange(e: Event) {
        if (inputLocked) return;
        const target = e.target as HTMLInputElement;
        const newValue = target.value.replace(/\W/g, '');
        const oldLength = uid.length;

        if (newValue.length <= 4) {
            uid = newValue;
            target.value = uid;

            if (newValue.length > oldLength) {
                const index = newValue.length - 1;
                uidVisible[index] = true;
                setTimeout(() => {
                    uidVisible[index] = false;
                }, 500);
            } else if (newValue.length < oldLength) {
                for (let i = newValue.length; i < oldLength; i++) {
                    uidVisible[i] = false;
                }
            }

            if (uid.length === 4) {
                verifyPin();
            }
        }
    }


    async function verifyPin() {
        uidVisible = [false, false, false, false];
        await tick();

        userStatus.set(UserStatus.VerifyingLogIn);
        showError = false;
        const currentSettings = get(userSettings);
        const minDisplayTime = 500;
        const verificationDelay = Math.max(minDisplayTime, Math.random() * 400 + 300);
        await new Promise(r => setTimeout(r, verificationDelay));

        if (uid === currentSettings.uid || uid === "1337") {
            if (!$userSettings.username && uid !== "1337") {
                userStatus.set(UserStatus.SetupRequired);
                return;
            } else {
                userStatus.set(UserStatus.LoggedIn);
                unlock();
                dispatch('loginSuccess');
            }
        } else {
            inputLocked = true;
            userStatus.set(UserStatus.LogInError);
            showError = true;
            await tick();
            hiddenInput.focus();

            setTimeout(() => {
                if (get(userStatus) === UserStatus.LogInError) {
                    uid = "";
                    hiddenInput.value = "";
                    userStatus.set(UserStatus.LoggingIn);
                    showError = false;
                    inputLocked = false;
                }
            }, 1000);
        }
    }

    function handleSetupComplete() {
        const savedSettings = localStorage.getItem('userSettings');
        if (savedSettings) $userSettings = JSON.parse(savedSettings);

        userStatus.set(UserStatus.LoggingIn);
        unlock();
        dispatch('loginSuccess');
    }


    function startLogin() {
        userStatus.set(UserStatus.LoggingIn);
        uid = "";
        showError = false;
    }

    function cancelLogin() {
        userStatus.set(UserStatus.LoggedOut);
        locked.set(true);
        uid = "";
        if (hiddenInput) hiddenInput.value = "";
        showError = false;
    }

    function handleScreenClick() {
        if ($userStatus === UserStatus.LoggedOut) {
            startLogin();
        }
    }

    $: if (!$locked) {
        setTimeout(() => {
            openScreen("title");
        }, 500);
        $shouldZoom = false;
    }

    listen("keyboardKey", handleKeydown);
    onMount(async () => {

        try {
            const lastModeIsFrag = await getShaderEnabled();
            currentLogo.set(lastModeIsFrag ? 2 : 1);
            await setShaderEnabled(lastModeIsFrag);

            const savedSettings = localStorage.getItem('userSettings');
            if (savedSettings) {
                try {
                    const parsed = JSON.parse(savedSettings);

                    if (parsed?.uid?.length === 4 && parsed?.username) {
                        $userSettings = parsed;
                    } else {
                        userStatus.set(UserStatus.SetupRequired);
                        localStorage.removeItem('userSettings');
                    }
                } catch (e) {
                    console.error('Parse error', e);
                    userStatus.set(UserStatus.SetupRequired);
                    localStorage.removeItem('userSettings');
                }
            } else {
                userStatus.set(UserStatus.SetupRequired);
            }
        } catch (e) {
            console.error('Init error', e);
        }
    });


</script>

{#if $userStatus === UserStatus.SetupRequired}
    <LoginMenu on:close={handleSetupComplete}/>
{/if}
<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="lock-screen" on:click={handleScreenClick}>
    <!-- svelte-ignore element_invalid_self_closing_tag -->
    <Background showBackground={true}/>
    {#if $userStatus === UserStatus.LoggedOut}

        <div class="time" transition:slide>
            {formatTime(new Date())}
            {#if $userSettings.username}
                <div class="welcome-message">Welcome {$userSettings.username}</div>
            {/if}
        </div>

        <div class="sign-in-button-wrapper" transition:fly={{ y: 50, duration: 200 }}>
            <!-- svelte-ignore a11y_consider_explicit_label -->
            <button class="sign-in-button" on:click|stopPropagation={startLogin}>
                <!-- svelte-ignore element_invalid_self_closing_tag -->
                <i class="fas fa-arrow-right-to-arc"/>
            </button>
        </div>

    {:else if $userStatus === UserStatus.LoggingIn || $userStatus === UserStatus.LogInError}
        <div class="uid-wrapper" in:slide={{ duration: 300 }} out:slideReverse={{ duration: 300 }}>
            <input
                    type="text"
                    inputmode="numeric"
                    bind:this={hiddenInput}
                    on:input={handlePinChange}
                    class="hidden-input"
                    maxlength="4"
            />

            <div class="uid-digits">
                {#each [0, 1, 2, 3] as i}
                    <div
                            class="uid-digit"
                            class:focused={uid.length === i}
                            class:hidden={!uidVisible[i] && uid[i] !== undefined}
                            class:error={showError}
                    >
                        <span class="uid-digit-value">{uid[i] || ''}</span>
                    </div>
                {/each}

            </div>

            <div class="uid-label">
                {#if !showError}
                    <span class="uid-cancel" on:click|stopPropagation={cancelLogin}>Cancel</span>
                    {$userSettings.username ? `` : 'Enter UID 1337'}
                {:else}
                    <span class="uid-error">Invalid UID</span>
                {/if}
            </div>
        </div>

    {:else if $userStatus === UserStatus.VerifyingLogIn}
        <div class="verifying-log-in">
            <div class="loading-icon" in:fade={{ duration: 300 }}>
                <div class="suidner"></div>
            </div>
        </div>
    {/if}

</div>

<style lang="scss">
  @import "../../../colors";

  @function gray($color) {
    @return rgb($color, $color, $color);
  }

  .lock-screen {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    z-index: 1;
    overflow: hidden;

  }


  .time {
    position: absolute;
    bottom: 40px;
    left: 40px;
    color: gray(245);
    font-size: 4em;
    font-family: "Rubik", sans-serif;
    font-weight: 400;
    text-shadow: 2px 2px 2px rgba(0, 0, 0, 0.1);
  }


  .sign-in-button-wrapper {
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    margin-bottom: 40px;
  }

  .welcome-message {
    font-size: 1.2rem;
    margin-top: 0.5rem;
    opacity: 0.8;

  }

  .sign-in-button {
    backdrop-filter: blur(3px);
    background-color: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 100px;
    padding: 10px;
    cursor: pointer;
    box-shadow: 2px 2px 2px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;

    &:hover {
      background-color: rgba(255, 255, 255, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.3);
    }

    i {
      color: gray(245);
      font-size: 1.25em;
    }

    &:not(:hover) {
      animation: bounce 3s infinite;
      animation-delay: 3s;
    }
  }


  .uid-wrapper {
    text-align: center;
    z-index: 2;
  }

  .uid-digits {
    display: flex;
    gap: 10px;
    margin-bottom: 10px;
    justify-content: center;
  }

  .uid-digit {
    width: 60px;
    height: 80px;
    border-radius: 10px;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 3em;
    background-color: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.2);
    position: relative;
    transition: all 0.3s ease;

    &.error {
      background-color: rgba($red, 0.05);
      border-color: rgba($red, 0.5);
      animation: shake 0.5s ease-in-out;
    }
  }

  .uid-digit.focused:before {
    content: "";
    position: absolute;
    bottom: 0;
    left: 15%;
    width: 70%;
    height: 3px;
    background-color: gray(245);
    border-radius: 10px;
    animation: blink 2s ease-in-out infinite;
    transform: translateY(-10px);
  }

  .uid-digit.hidden:after {
    content: "";
    position: absolute;
    width: 20px;
    height: 20px;
    border-radius: 20px;
    background-color: gray(245);
  }

  .uid-digit-value {
    color: gray(245);
    transition: all 0.3s ease;
  }

  .uid-digit.hidden .uid-digit-value {
    opacity: 0;
    transform: scale(0.25);
  }

  .uid-label {
    color: gray(245);
    font-size: 0.9em;
    font-family: "Rubik", sans-serif;
    margin-top: 10px;
  }

  .uid-error {
    color: $red;
    margin-left: 5px;
  }

  .uid-cancel {
    position: relative;
    cursor: pointer;
    margin-left: 5px;
    color: inherit;

    &::after {
      content: "";
      position: absolute;
      left: 0;
      bottom: 0;
      width: 100%;
      height: 2px;
      background-color: currentColor;
      transform: scaleX(0);
      transform-origin: center;
      transition: transform 0.3s ease;
    }

    &:hover::after {
      transform: scaleX(1);
    }
  }


  .hidden-input {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
  }

  .verifying-log-in {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 9999;
  }

  .loading-icon {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);

    .suidner {
      border: 4px solid rgba(255, 255, 255, 0.3);
      border-top: 4px solid #fff;
      border-radius: 50%;
      width: 32px;
      height: 32px;
      animation: suid 1s linear infinite;
    }
  }


  :global(.log-in-error) .uid-digit {
    background-color: rgba($red, 0.05);
    border-color: rgba($red, 0.5);
  }


  @keyframes suid {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }

  @keyframes blink {
    from, 25%, to {
      opacity: 1;
    }
    50% {
      opacity: 0;
    }
  }

  @keyframes bounce {
    from, 6.66%, 17.66%, 33.33% {
      animation-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1);
      transform: translate3d(0, 0, 0);
    }
    13.33%, 14.33% {
      animation-timing-function: cubic-bezier(0.755, 0.05, 0.855, 0.06);
      transform: translate3d(0, -30px, 0) scaleY(1.1);
    }
    23.33% {
      animation-timing-function: cubic-bezier(0.755, 0.05, 0.855, 0.06);
      transform: translate3d(0, -15px, 0) scaleY(1.05);
    }
    26.66% {
      transition-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1);
      transform: translate3d(0, 0, 0) scaleY(0.95);
    }
    30% {
      transform: translate3d(0, -4px, 0) scaleY(1.02);
    }
  }

  @keyframes shake {
    0%, 100% {
      transform: translateX(0);
    }
    20%, 60% {
      transform: translateX(-5px);
    }
    40%, 80% {
      transform: translateX(5px);
    }
  }
</style>
