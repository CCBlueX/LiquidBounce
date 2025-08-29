<script lang="ts">
    import ToolTip from "../../ToolTip.svelte";
    import {
        getAccounts,
        getSession,
        openScreen,
        directLoginToCrackedAccount,
        randomUsername,
        loginToAccount as loginToAccountRest

    } from "../../../../../integration/rest";
    import {onMount} from "svelte";
    import {listen} from "../../../../../integration/ws";
    import {location} from "svelte-spa-router";
    import {quintOut} from "svelte/easing";
    import {fade, slide} from "svelte/transition";
    import type {Account} from "../../../../../integration/types";
    import Avatar from "./Avatar.svelte";
    import {notification} from "../notification_store";
    import AddAccountModal from "../../../altmanager/addaccount/AddAccountModal.svelte";
    import type {
        AccountManagerAdditionEvent,
        AccountManagerLoginEvent,
        AccountManagerMessageEvent
    } from "../../../../../integration/events";
    import DirectLoginModal from "../../../altmanager/directLogin/DirectLoginModal.svelte";

    let username = "";
    let expanded = false;
    let accountElement: HTMLElement;
    let headerElement: HTMLElement;
    let searchQuery = "";
    let accounts: Account[] = [];
    const userData = JSON.parse(
        localStorage.getItem('userSettings') ||
        JSON.stringify({
            username: 'Customer',
            uid: '0000',
            isDev: false,
            isOwner: false,
            hwid: '',
            developer: 'Customer',
            avatar: ''
        })
    );


    let addAccountModalVisible = false;
    let directLoginModalVisible = false;
    $: renderedAccounts = accounts.filter(a => a.username.toLowerCase().includes(searchQuery.toLowerCase()) || searchQuery === "");

    const inAccountManager = $location === "/altmanager";

    async function refreshSession() {
        const session = await getSession();
        username = session.username;
    }

    async function refreshAccounts() {
        accounts = await getAccounts();
    }


    listen("session", async () => {
        await refreshSession();
    });

    listen("accountManagerRemoval", async () => {
        await refreshAccounts();
    });

    listen("accountManagerAddition", async () => {
        await refreshAccounts();
    });
    listen("accountManagerAddition", (e: AccountManagerAdditionEvent) => {
        addAccountModalVisible = false;
        refreshAccounts();
        notification.set(null);
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
        directLoginModalVisible = false;
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

    function handleWindowClick(e: MouseEvent) {
        if (!accountElement.contains(e.target as Node)) {
            expanded = false;
            searchQuery = "";
        }
    }

    function handleSelectClick(e: MouseEvent) {
        if (!expanded) {

            expanded = !(e.target as HTMLElement).classList.contains("icon");
        } else {
            expanded = !headerElement.contains(e.target as Node);
        }

        if (!expanded) {
            searchQuery = "";
        }
    }

    async function logging(id: number) {
        let showNotification = true;
        const notificationTimeout = setTimeout(() => {
            if (showNotification) {
                notification.set({
                    title: "AltManager",
                    message: "Logging in...",
                    error: false
                });
            }
        }, 50);

        try {
            await loginToAccountRest(id);

            showNotification = false;
            clearTimeout(notificationTimeout);
        } catch (error) {
            showNotification = false;
            clearTimeout(notificationTimeout);
            throw error;
        }
    }

    async function loginWithRandomUsername() {
        const username = await randomUsername();
        await directLoginToCrackedAccount(username, false);
    }

    function copyHWID() {
        navigator.clipboard.writeText(userData.hwid)
            .then(() => {
                notification.set({
                    title: "HWID Copied",
                    message: "HWID has been copied to clipboard!",
                    error: false
                });
            })
            .catch(() => {
                notification.set({
                    title: "Error",
                    message: "Failed to copy HWID",
                    error: true
                });
            });
    }

    onMount(async () => {
        await refreshSession();
        await refreshAccounts();
    });
</script>


<svelte:window on:click={handleWindowClick}/>
<AddAccountModal bind:visible={addAccountModalVisible}/>
<DirectLoginModal bind:visible={directLoginModalVisible}/>
<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div bind:this={accountElement} class="account-selector" class:expanded on:click={handleSelectClick}>
    <div bind:this={headerElement} class="selector-header">
        <div class="avatar-container">
            <!-- svelte-ignore a11y_missing_attribute -->
            <img alt="avatar" class="avatar" draggable="false" src={userData.avatar|| 'img/avatars/Customer.png'}/>
        </div>
        <div class="account-info">
            <div class="username" on:dblclick|stopPropagation={copyHWID} style="cursor: pointer">
                {#if userData.isOwner || userData.isDev}
                    {userData.developer || 'Developer'}
                {:else}
                    {userData.username || 'Customer'}
                {/if}
            </div>

            {#if userData.isOwner}
                <span class="owner-badge">Owner</span>
            {:else if userData.isDev}
                <span class="developer-badge">Developer</span>
            {:else}
                <span class="customer-badge">Free User</span>
            {/if}
        </div>

        <div class="action-buttons">
            <button class="icon-button" on:click={loginWithRandomUsername} type="button">
                <ToolTip text="Random username"/>

                <img alt="random username" class="icon" draggable="false" src="img/menu/account/icon-random.svg">
            </button>
            <button class="icon-button" disabled={inAccountManager} on:click={() => openScreen("altmanager")}
                    type="button">
                <ToolTip text="Change account"/>

                <img alt="change account" class="icon" draggable="false" src="img/menu/icon-pen.svg">
            </button>
        </div>
    </div>

    {#if expanded}
        <div class="account-dropdown" transition:fade|global={{ duration: 150, easing: quintOut }}>
            <div class="search-container" in:slide|global={{ duration: 150, easing: quintOut }}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" class="search-icon">
                    <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="2"/>
                    <path d="M21 21L17 17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
                <!-- svelte-ignore a11y_autofocus -->
                <input type="text" autofocus class="search-input" placeholder="Search accounts..."
                       bind:value={searchQuery}>
            </div>

            <div class="account-list-container" in:slide|global={{ duration: 150, easing: quintOut, delay: 50 }}>
                {#if accounts.length > 0}
                    {#if renderedAccounts.length > 0}
                        <div class="account-list">
                            {#each renderedAccounts as a (a.id)}
                                <div on:click={() => logging(a.id)} class="account-item"
                                     transition:slide|global={{ duration: 150, easing: quintOut }}
                                     class:active={a.username === username}>
                                    <Avatar url={a.avatar}/>
                                    <div class="account-details">
                                        <div class="account-username">{a.username}</div>
                                        <div class="account-type">{a.type}</div>
                                    </div>
                                    {#if a.username === username}
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" class="check-icon">
                                            <path d="M20 6L9 17L4 12" stroke="currentColor" stroke-width="2"
                                                  stroke-linecap="round" stroke-linejoin="round"/>
                                        </svg>
                                    {/if}
                                </div>
                            {/each}
                        </div>
                    {:else}
                        <div class="empty-state" in:fade>
                            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" class="empty-icon">
                                <path d="M3 7V17C3 18.1046 3.89543 19 5 19H19C20.1046 19 21 18.1046 21 17V7C21 5.89543 20.1046 5 19 5H5C3.89543 5 3 5.89543 3 7Z"
                                      stroke="currentColor" stroke-width="2"/>
                                <path d="M7 10L12 13L17 10" stroke="currentColor" stroke-width="2"
                                      stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                            <div class="empty-text">No accounts found</div>
                        </div>
                    {/if}
                {:else}
                    <div class="empty-state" transition:fade>
                        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" class="empty-icon">
                            <path d="M12 12C14.2091 12 16 10.2091 16 8C16 5.79086 14.2091 4 12 4C9.79086 4 8 5.79086 8 8C8 10.2091 9.79086 12 12 12Z"
                                  stroke="currentColor" stroke-width="2"/>
                            <path d="M19 21V19C19 17.3431 17.6569 16 16 16H8C6.34315 16 5 17.3431 5 19V21"
                                  stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                        </svg>
                        <div class="empty-text">No accounts added yet</div>
                        <button class="add-account-button" on:click={() => addAccountModalVisible = true}>
                            Add Account
                        </button>
                    </div>
                {/if}
            </div>
        </div>
    {/if}
</div>

<style lang="scss">
    @use "../../../../../colors" as *;;

  $border-radius: 12px;
  $transition-speed: 0.2s;
  $shadow: 0 8px 24px rgba(0, 0, 0, 0.12);

  .account-selector {
    width: 360px;
    position: relative;
    user-select: none;

    &.expanded {
      .selector-header {
        border-radius: $border-radius $border-radius 0 0;
        background: linear-gradient(
                        to bottom,
                        rgba(255, 255, 255, 0.05) 0%,
                        rgba(0, 0, 0, 0.2) 100%
        );
        box-shadow: $shadow;
      }
    }
  }

  .selector-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    background: rgba(255, 255, 255, 0.05);
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
    border-radius: $border-radius;
    transition: all $transition-speed ease;
    cursor: pointer;

    &:hover {
      background: rgba(255, 255, 255, 0.05);
    }
  }

  .avatar-container {
    position: relative;
    flex-shrink: 0;
  }

  .avatar {
    width: 68px;
    height: 68px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid rgba(white, 0.1);
    transition: all $transition-speed ease;


  }


  .account-info {
    flex-grow: 1;
    min-width: 0;
  }

  .username {
    font-weight: 600;
    font-size: 20px;
    color: $text;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .account-type {
    font-weight: bold;
    font-size: 20px;
    margin-top: 2px;
  }

  .owner-badge {
    color: $menu-account-owner;
  }

  .developer-badge {
    color: $menu-account-developer;
  }

  .customer-badge {
    color: $menu-account-customer;
  }

  .action-buttons {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }


  .icon-button {
    background-color: transparent;
    border: none;
    position: relative;
    height: max-content;
    cursor: pointer;
    display: flex;
    align-items: center;

    .icon {
      filter: drop-shadow(0 0 4px rgba($base, 0.5));
    }

    &:disabled {
      pointer-events: none;
      opacity: .5;
    }
  }


  .account-dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    background: rgba(0, 0, 0, 0.15);
    backdrop-filter: blur(12px);
    border-radius: 0 0 $border-radius $border-radius;
    overflow: hidden;
    box-shadow: $shadow;
    z-index: 1000;
  }

  .search-container {
    position: relative;
    padding: 12px 16px;
    border-bottom: 1px solid rgba($text, 0.1);
  }

  .search-icon {
    position: absolute;
    left: 28px;
    top: 50%;
    transform: translateY(-50%);
    color: rgba($text, 0.6);
  }

  .search-input {
    width: 100%;
    padding: 8px 16px 8px 40px;
    background: rgba($text, 0.05);
    border: none;
    border-radius: 6px;
    color: $text;
    font-size: 14px;
    transition: all $transition-speed ease;

    &:focus {
      outline: none;
      background: rgba($text, 0.1);
    }

    &::placeholder {
      color: rgba($text, 0.5);
    }
  }

  .account-list-container {
    max-height: 400px;
    overflow-y: auto;
    padding: 8px 0;
  }

  .account-list {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .account-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 16px;
    cursor: pointer;
    transition: all $transition-speed ease;
    font-weight: 600;

    &:hover {
      background: rgba($text, 0.05);
    }

    &.active {
      background: rgba($text, 0.1);

      .account-username {
        color: $text;
      }
    }
  }

  .account-details {
    flex-grow: 1;
    min-width: 0;
  }

  .account-username {
    font-size: 14px;
    color: $text;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .account-type {
    font-size: 12px;
    color: rgba($text, 0.6);
    margin-top: 2px;
  }

  .check-icon {
    flex-shrink: 0;
    color: $text;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 24px 16px;
    text-align: center;
    color: rgba($text, 0.7);
  }

  .empty-icon {
    margin-bottom: 12px;
    color: rgba($text, 0.5);
  }

  .empty-text {
    font-size: 14px;
    margin-bottom: 16px;
  }

  .add-account-button {
    background: rgba($text, 0.1);
    color: $text;
    border: none;
    border-radius: 6px;
    padding: 8px 16px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all $transition-speed ease;

    &:hover {
      background: rgba($text, 0.2);
    }
  }
</style>
