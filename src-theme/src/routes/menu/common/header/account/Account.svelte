<script lang="ts">
    import ToolTip from "../../ToolTip.svelte";
    import {
        getAccounts,
        getSession,
        openScreen,
        loginToAccount,
        directLoginToCrackedAccount,
        randomUsername
    } from "../../../../../integration/rest";
    import {onMount} from "svelte";
    import {listen} from "../../../../../integration/ws";
    import {location} from "svelte-spa-router";
    import {quintOut} from "svelte/easing";
    import {fade, slide} from "svelte/transition";
    import type {Account} from "../../../../../integration/types";
    import Avatar from "./Avatar.svelte";
    import {notification} from "../notification_store";
    import RippleLoader from "../../RippleLoader.svelte";
    import {isLoggingIn} from "../../../altmanager/altmanager_store";

    let username = "";
    let avatar = "";
    let premium = true;

    let expanded = false;
    let accountElement: HTMLElement;
    let headerElement: HTMLElement;

    let searchQuery = "";
    let accounts: Account[] = [];

    $: renderedAccounts = accounts.filter(a => a.username.toLowerCase().includes(searchQuery.toLowerCase()) || searchQuery === "");

    const inAccountManager = $location === "/altmanager";

    async function refreshSession() {
        const session = await getSession();
        username = session.username;
        avatar = session.avatar;
        premium = session.premium;
    }

    async function refreshAccounts() {
        accounts = await getAccounts();
    }

    onMount(async () => {
        await refreshSession();
        await refreshAccounts();
    });

    listen("session", async () => {
        await refreshSession();
    });

    listen("accountManagerRemoval", async () => {
        await refreshAccounts();
    });

    listen("accountManagerAddition", async () => {
        await refreshAccounts();
    });

    function handleWindowClick(e: MouseEvent) {
        if (!accountElement.contains(e.target as Node)) {
            expanded = false;
            searchQuery = "";
        }
    }

    function handleSelectClick(e: MouseEvent) {
        if (!expanded) {
            // Prevent icon buttons from opening quick switcher
            expanded = !(e.target as HTMLElement).classList.contains("icon");
        } else {
            expanded = !headerElement.contains(e.target as Node);
        }

        if (!expanded) {
            searchQuery = "";
        }
    }

    async function login(account: Account) {
        notification.set({
            title: "AltManager",
            message: "Logging in...",
            error: false
        });

        await loginToAccount(account.id);
    }

    async function loginWithRandomUsername() {
        const username = await randomUsername();
        await directLoginToCrackedAccount(username, false);
    }
</script>

<svelte:window on:click={handleWindowClick}/>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="account" class:expanded bind:this={accountElement} on:click={handleSelectClick}>
    <div class="header" bind:this={headerElement}>
        {#if $isLoggingIn}
            <div class="avatar" transition:fade={{ duration: 200 }}>
                <RippleLoader size={68} />
            </div>
        {:else}
            <object data={avatar} type="image/png" class="avatar" aria-label="avatar" in:fade={{ duration: 200, delay: 200 }}>
                <img src="img/steve.png" alt=avatar class="avatar">
            </object>
        {/if}
        <div class="username">{username}</div>
        <div class="account-type">
            {#if premium}
                <span class="premium">Premium</span>
            {:else}
                <span class="offline">Offline</span>
            {/if}
        </div>
        <div class="buttons">
            <button class="icon-button" type="button" on:click={loginWithRandomUsername}>
                <ToolTip text="Random username"/>

                <img class="icon" src="img/menu/account/icon-random.svg" alt="random username">
            </button>
            <button class="icon-button" disabled={inAccountManager} type="button"
                    on:click={() => openScreen("altmanager")}>
                <ToolTip text="Change account"/>

                <img class="icon" src="img/menu/icon-pen.svg" alt="change account">
            </button>
        </div>
    </div>

    {#if expanded}
        <div class="quick-switcher" transition:fade|global={{ duration: 200, easing: quintOut }}>
            <!-- svelte-ignore a11y_autofocus -->
            <input type="text" autofocus class="account-search" placeholder="Search..." bind:value={searchQuery}>

            {#if accounts.length > 0}
                {#if renderedAccounts.length > 0}
                    <div class="account-list">
                        {#each renderedAccounts as a}
                            <div on:click={() => login(a)} class="account-item"
                                 transition:slide|global={{ duration: 200, easing: quintOut }}
                                 class:active={a.username === username}>
                                <Avatar url={a.avatar}/>
                                <div class="username">{a.username}</div>
                                <div class="type">{a.type}</div>
                            </div>
                        {/each}
                    </div>
                {:else}
                    <div class="placeholder">No results</div>
                {/if}
            {:else}
                <div class="placeholder">Account list is empty</div>
            {/if}
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../../../colors" as *;

  .account {
    width: 488px;
    position: relative;

    &.expanded {
      .header {
        border-radius: 5px 5px 0 0;
      }
    }
  }

  .header {
    background-color: rgba($hotbar-base-color, 0.68);
    padding: 15px 18px;
    border-radius: 5px;
    align-items: center;
    display: grid;
    grid-template-areas:
        "a b c"
        "a d c";
    grid-template-columns: max-content 1fr max-content;
    column-gap: 15px;
    cursor: pointer;
    transition: ease border-radius .2s;

    .avatar {
      height: 68px;
      width: 68px;
      border-radius: 50%;
      grid-area: a;
    }

    .username {
      font-weight: 600;
      color: $menu-text-color;
      font-size: 20px;
      grid-area: b;
      align-self: flex-end;
    }

    .account-type {
      font-weight: 500;
      font-size: 20px;
      grid-area: d;
      align-self: flex-start;

      .premium {
        color: $menu-account-premium-color;
      }

      .offline {
        color: $menu-text-dimmed-color;
      }
    }

    .buttons {
      grid-area: c;
      display: flex;
      column-gap: 20px;
      align-items: center;
    }

    .icon-button {
      background-color: transparent;
      border: none;
      position: relative;
      height: max-content;
      cursor: pointer;
      display: flex;
      align-items: center;

      &:disabled {
        pointer-events: none;
        opacity: .5;
      }
    }
  }

  .quick-switcher {
    position: absolute;
    z-index: 1000;
    width: 100%;
    border-radius: 0 0 5px 5px;
    background-color: rgba($menu-base-color, 0.9);

    .placeholder {
      font-weight: 500;
      font-size: 20px;
      color: $menu-text-dimmed-color;
      padding: 15px 20px;
    }

    .account-search {
      background-color: rgba($menu-base-color, .36);
      border: none;
      color: $menu-text-color;
      font-family: "Inter", sans-serif;
      padding: 15px 15px 15px 50px;
      width: 100%;
      font-size: 18px;
      border-bottom: solid 4px $accent-color;
      background-image: url("/img/menu/icon-search.svg");
      background-repeat: no-repeat;
      background-position: 18px center;
      background-size: 18px 18px;
    }

    .account-list {
      max-height: 350px;
      overflow: auto;
    }

    .account-item {
      color: $menu-text-dimmed-color;
      font-size: 20px;
      padding: 15px 20px;
      transition: ease color .2s;
      cursor: pointer;
      display: grid;
      grid-template-areas:
        "a b"
        "a c";
      grid-template-columns: max-content 1fr;
      column-gap: 15px;

      .username {
        grid-area: b;
        font-weight: 600;
        font-size: 20px;
        transition: ease color .2s;
      }

      .type {
        grid-area: c;
      }

      &:hover {
        color: $menu-text-color;
      }

      &.active {
        .username {
          color: $accent-color;
        }
      }
    }
  }
</style>
