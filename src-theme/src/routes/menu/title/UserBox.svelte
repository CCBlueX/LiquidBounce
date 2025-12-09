<script lang="ts">
    import {onMount} from "svelte";
    import {listen} from "../../../integration/ws";
    import {getClientUser, loginClientUser, logoutClientUser} from "../../../integration/rest";
    import type {ClientUser} from "../../../integration/types";
    import ToolTip from "../common/ToolTip.svelte";

    let user: ClientUser | null = null;
    let loading = true;

    async function refreshUser() {
        loading = true;
        try {
            user = await getClientUser();
        } catch (e) {
            console.error("Failed to get client user:", e);
            user = null;
        } finally {
            loading = false;
        }
    }

    async function login() {
        try {
            await loginClientUser();
        } catch (e) {
            console.error("Login failed:", e);
            loading = false;
        }
    }

    async function logout() {
        try {
            await logoutClientUser();
        } catch (e) {
            console.error("Logout failed:", e);
            loading = false;
        }
    }

    onMount(async () => {
        try {
            await refreshUser();
        } catch (e) {
            loading = false;
        }
    });

    listen("userLoggedIn", refreshUser);
    listen("userLoggedOut", () => {
        user = null;
        loading = false;
    });

    onMount(() => {
        const timeout = setTimeout(() => {
            if (loading) loading = false;
        }, 3000);
        return () => clearTimeout(timeout);
    });

    $: avatarUrl = user?.nickname 
        ? `https://avatar.liquidbounce.net/avatar/${encodeURIComponent(user.nickname)}` 
        : "img/steve.png";
    
    function formatGroups(groups: string[]): string {
        if (!groups || groups.length === 0) return "User";
        if (groups.length === 1) return groups[0];
        return `${groups[0]} +${groups.length - 1}`;
    }
</script>

<div class="user-box">
    <div class="user-info">
        <div class="avatar-container">
            <img src={avatarUrl} alt="avatar" class="avatar {user ? '' : 'steve-avatar'}">
            {#if user?.premium}
                <div class="premium-badge">
                    <img src="img/menu/icon-star.svg" alt="premium" class="star-icon">
                </div>
            {/if}
        </div>
        <div class="user-details">
            <div class="user-main-text">
                {user ? (user.nickname || user.name || "Unknown") : loading ? "Loading..." : "Sign in to LiquidBounce"}
            </div>
            {#if user}
                <div class="groups" title={user.groups.join(", ")}>
                    {formatGroups(user.groups)}
                </div>
            {/if}
        </div>
        {#if !loading}
            <button class="icon-button" on:click={user ? logout : login}>
                <ToolTip text={user ? "Logout" : "Login"} />
                <img src={user ? "img/menu/icon-back.svg" : "img/menu/altmanager/icon-login.svg"} alt={user ? "logout" : "login"} class="icon">
            </button>
        {/if}
    </div>
</div>

<style lang="scss">
    @use "../../../colors.scss" as *;

    .user-box {
        background-color: rgba($menu-base-color, 0.68);
        width: 590px;
        padding: 25px 35px;
        border-radius: 5px;
        display: flex;
        align-items: center;
        margin-bottom: 25px;
        transition: background-color 0.2s ease-out;
        cursor: pointer;

        &:hover {
            background-color: rgba(lighten($menu-base-color, 5%), 0.68);
        }
    }

    .user-info {
        display: flex;
        align-items: center;
        gap: 25px;
        width: 100%;
    }

    .avatar-container {
        position: relative;
        flex-shrink: 0;
        width: 90px;
        height: 90px;
        border-radius: 50%;
        background-color: $accent-color;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .avatar {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 50%;
        overflow: hidden;
    }

    .steve-avatar {
        width: 100%;
        height: 100%;
        object-fit: contain;
        image-rendering: pixelated;
    }

    .premium-badge {
        position: absolute;
        top: 3px;
        right: -2px;
        width: 34px;
        height: 34px;
        display: flex;
        align-items: center;
        justify-content: center;

        .star-icon {
            width: 30px;
            height: 30px;
            filter: drop-shadow(0px 0px 2px rgba(0, 0, 0, 0.5));
        }
    }

    .user-details {
        flex: 1;
        min-width: 0;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 90px;
    }

    .user-main-text {
        font-weight: 600;
        color: $menu-text-color;
        font-size: 26px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        width: 100%;
        text-align: center;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .groups {
        font-size: 18px;
        color: $menu-text-dimmed-color;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        text-align: center;
        width: 100%;
    }

    .icon-button {
        background: none;
        border: none;
        cursor: pointer;
        padding: 8px;
        color: $menu-text-dimmed-color;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: color 0.2s;
        flex-shrink: 0;
        position: relative;

        &:hover {
            color: $menu-text-color;
        }

        .icon {
            width: 28px;
            height: 28px;
        }
    }
</style>
