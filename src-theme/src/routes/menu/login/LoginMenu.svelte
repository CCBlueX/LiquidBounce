<script lang="ts">
    import {slide} from 'svelte/transition';
    import {createEventDispatcher, onDestroy, onMount} from 'svelte';
    import {getVerification} from "../../../integration/rest";
    import {userSettings} from './userSettings';

    const dispatch = createEventDispatcher();

    interface UserSettings {
        username: string;
        uid: string;
        isDev: boolean;
        isOwner: boolean;
        hwid: string;
        developer: string;
        avatar: string;
    }

    let settings: UserSettings | undefined;
    const unsubscribe = userSettings.subscribe(value => {
        settings = value;
    });
    let username = '';
    let password = '';
    let confirmPassword = '';
    let accessKey = '';
    let error = '';
    let uid = '';
    let showUidNotification = false;
    let showWelcomeNotification = false;
    let countdown = 3;
    let isTyping = false;
    let currentInputIndex = 0;
    const inputIds = ['username', 'password', 'confirmPassword', 'accessKey'];

    const DEFAULT_ACCESS_KEY = '0721';

    function generateUid() {
        return 'xxxx'.replace(/x/g, () => {
            return Math.floor(Math.random() * 16).toString(16);
        });
    }

    async function register() {
        error = '';

        if (!username.trim()) {
            error = 'Username is required';
            focusInput('username');
            return;
        }
        if (password.length < 6) {
            error = 'Password must be at least 6 characters';
            focusInput('password');
            return;
        }
        if (password !== confirmPassword) {
            error = 'Passwords do not match';
            focusInput('confirmPassword');
            return;
        }
        if (accessKey.trim() !== DEFAULT_ACCESS_KEY) {
            error = 'Invalid access key';
            focusInput('accessKey');
            return;
        }

        try {
            const verification = await getVerification();
            const assignedUid = (verification.isDev || verification.isOwner || verification.hwid) ? '0000' : generateUid();

            const newUserSettings = {
                username: username.trim(),
                uid: assignedUid,
                isDev: verification.isDev,
                isOwner: verification.isOwner,
                hwid: verification.hwid,
                developer: verification.developer,
                avatar: verification.avatar
            };
            localStorage.setItem('userSettings', JSON.stringify(newUserSettings));
            userSettings.set(newUserSettings);

            uid = newUserSettings.uid;
            if (verification.isDev || verification.isOwner || verification.hwid) {
                showWelcomeNotification = true;

                const timer = setInterval(() => {
                    countdown--;
                    if (countdown <= 0) {
                        clearInterval(timer);
                        closeNotification();
                    }
                }, 1000);
            } else {
                showUidNotification = true;
            }

            dispatch('registered', newUserSettings);
        } catch (e) {
            error = 'Failed to verify account. Please try again.';
            console.error("Verification error:", e);
        }
    }

    async function simulateTyping(target: string, field: 'username' | 'password' | 'confirmPassword' | 'accessKey') {
        isTyping = true;
        let current = '';

        for (const char of target) {
            await new Promise(resolve => setTimeout(resolve, 30 + Math.random() * 10));
            current += char;
            if (field === 'username') username = current;
            else if (field === 'password') password = current;
            else if (field === 'confirmPassword') confirmPassword = current;
            else if (field === 'accessKey') accessKey = current;
        }
        isTyping = false;
    }


    function handleNativeKeyDown(e: KeyboardEvent) {

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            if (!isTyping) navigateInputs(1);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            if (!isTyping) navigateInputs(-1);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (!isTyping) register();
        }
    }

    function navigateInputs(direction: number) {
        currentInputIndex = (currentInputIndex + direction + inputIds.length) % inputIds.length;
        focusInput(inputIds[currentInputIndex]);
    }

    function focusInput(id: string) {
        const inputElement = document.getElementById(id) as HTMLInputElement;
        if (inputElement) {
            inputElement.focus();
            currentInputIndex = inputIds.indexOf(id);
        }
    }

    function closeNotification() {
        showUidNotification = false;
        showWelcomeNotification = false;
        countdown = 3;
        dispatch('close');
    }

    onMount(async () => {
        focusInput('username');
        window.addEventListener('keydown', handleNativeKeyDown);

        try {
            const verification = await getVerification();
            if (verification.isDev || verification.isOwner || verification.hwid) {
                await simulateTyping(verification.developer, 'username');
                await simulateTyping('0d000721', 'password');
                await simulateTyping('0d000721', 'confirmPassword');
                await simulateTyping('0721', 'accessKey');
            }
        } catch (e) {
            console.error("Non-developer login:", e);
        }
    });

    onDestroy(() => {
        unsubscribe();
        window.removeEventListener('keydown', handleNativeKeyDown);
    });
</script>

<div class="register-menu">
    <div class="menu-content" transition:slide={{ duration: 300 }}>
        <h2 class="title">Create Account</h2>

        {#if error}
            <div class="error-message">{error}</div>
        {/if}

        <div class="form-group">
            <label for="username">Username</label>
            <input
                    bind:value={username}
                    class:error={error.includes('Username')}
                    id="username"
                    on:focus={() => currentInputIndex = 0}
                    placeholder="Enter your username"
                    readonly={isTyping}
                    type="text"
            />
        </div>

        <div class="form-group">
            <label for="password">Password</label>
            <input
                    bind:value={password}
                    class:error={error.includes('Password')}
                    id="password"
                    on:focus={() => currentInputIndex = 1}
                    placeholder="At least 6 characters"
                    readonly={isTyping}
                    type="password"
            />
        </div>

        <div class="form-group">
            <label for="confirmPassword">Confirm Password</label>
            <input
                    bind:value={confirmPassword}
                    class:error={error.includes('match')}
                    id="confirmPassword"
                    on:focus={() => currentInputIndex = 2}
                    placeholder="Re-enter your password"
                    readonly={isTyping}
                    type="password"
            />
        </div>

        <div class="form-group">
            <label for="accessKey">Access Key</label>
            <input
                    bind:value={accessKey}
                    class:error={error.includes('access key')}
                    id="accessKey"
                    maxlength="10"
                    on:focus={() => currentInputIndex = 3}
                    placeholder="Enter access key"
                    readonly={isTyping}
                    type="text"
            />
        </div>

        <button class="register-button" disabled={isTyping} on:click={register}>
            {isTyping ? 'Registering...' : 'Register'}
        </button>
    </div>
</div>

{#if showUidNotification}
    <div class="notification" transition:slide>
        <div class="notification-content">
            <div class="notification-title">Registration Successful!</div>
            <div class="notification-message">
                Your unique ID: <span class="uid-highlight">{uid}</span>
            </div>
        </div>
        <button class="notification-close" on:click={closeNotification}>
            ✕
        </button>
    </div>
{/if}

{#if showWelcomeNotification}
    <div class="notification welcome-notification" transition:slide>
        <div class="notification-content">
            <div class="notification-title">Welcome {settings?.developer}!</div>
            <div class="notification-message">
                will close in {countdown} seconds..
            </div>
        </div>
    </div>
{/if}

<style>
    :global(body) {
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    }

    .register-menu {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(8px);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 100;
    }

    .menu-content {
        background: #1e1e2e;
        padding: 2rem;
        border-radius: 12px;
        width: 380px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .title {
        color: #fff;
        text-align: center;
        margin-bottom: 1.5rem;
        font-size: 1.5rem;
        font-weight: 600;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        padding-bottom: 0.5rem;
    }

    .form-group {
        margin-bottom: 1.25rem;
    }

    label {
        display: block;
        color: #a1a1aa;
        margin-bottom: 0.5rem;
        font-size: 0.875rem;
        font-weight: 500;
    }

    input {
        width: 100%;
        padding: 0.75rem;
        border: 1px solid #3f3f46;
        border-radius: 8px;
        background: #27272a;
        color: #f4f4f5;
        font-size: 0.9375rem;
        transition: all 0.2s ease;
    }

    input::placeholder {
        color: #71717a;
    }

    input:focus {
        outline: none;
        border-color: #818cf8;
        box-shadow: 0 0 0 2px rgba(129, 140, 248, 0.2);
    }

    input.error {
        border-color: #f87171;
    }

    .error-message {
        color: #f87171;
        font-size: 0.875rem;
        text-align: center;
        margin-bottom: 1rem;
        padding: 0.5rem;
        background: rgba(248, 113, 113, 0.1);
        border-radius: 6px;
    }

    .register-button {
        width: 100%;
        padding: 0.75rem;
        background: #6366f1;
        color: #fff;
        border: none;
        border-radius: 8px;
        cursor: pointer;
        font-size: 1rem;
        font-weight: 500;
        transition: background 0.2s;
        margin-top: 0.5rem;
    }

    .register-button:hover {
        background: #818cf8;
    }

    .notification {
        position: fixed;
        top: 20px;
        right: 10px;
        background: #1e1e2e;
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        border: 1px solid rgba(255, 255, 255, 0.1);
        display: flex;
        align-items: center;
        gap: 12px;
        z-index: 1000;
    }

    .notification-content {
        flex: 1;
    }

    .notification-title {
        font-weight: 600;
        margin-bottom: 4px;
    }

    .notification-message {
        font-size: 0.875rem;
        color: #a1a1aa;
    }

    .notification-close {
        background: none;
        border: none;
        color: #a1a1aa;
        cursor: pointer;
        padding: 4px;
    }

    .uid-highlight {
        background: rgba(99, 102, 241, 0.2);
        padding: 2px 6px;
        border-radius: 4px;
        font-family: monospace;
    }

    .welcome-notification .notification-title {
        color: #fff;
    }

    .welcome-notification .notification-message {
        color: #e0e0e0;
    }
</style>
