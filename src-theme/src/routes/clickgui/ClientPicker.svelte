<script lang="ts">
    import {onMount} from "svelte";
    import {fly} from "svelte/transition";
    import {quintOut} from "svelte/easing";
    import type {Account, Client, ModuleSetting} from "../../integration/types";
    import {activateClient, closeClient, getAccounts, getClients, openClient} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {ClientsUpdateEvent, ModuleActivationEvent} from "../../integration/events";
    import {escapeLayer} from "../../integration/escape";
    import {pickedClient} from "./clickgui_store";
    import ClientView from "../../components/ClientView.svelte";
    import SettingButton from "./setting/common/SettingButton.svelte";
    import Dropdown from "./setting/common/Dropdown.svelte";
    import TextSetting from "./setting/TextSetting.svelte";

    const MAIN_CLIENT = 0;

    let clients = $state<Client[]>([]);
    let accounts = $state<Account[]>([]);
    let expanded = $state(false);

    let accountName = $state("");
    let address = $state<ModuleSetting>({
        name: "Server",
        valueType: "TEXT",
        value: "",
        description: undefined,
        key: undefined
    });
    let adding = $state(false);
    let error = $state<string | null>(null);

    const active = $derived(clients.find(client => client.active));
    const shown = $derived(clients.find(client => client.id === $pickedClient) ?? active);
    const accountNames = $derived(accounts.map(accountLabel));

    function accountLabel(account: Account): string {
        const namesakes = accounts.filter(other => other.username === account.username).length;
        return namesakes > 1 ? `${account.username} (${account.type})` : account.username;
    }

    function update(next: Client[]) {
        clients = next;

        // Picking the active client is the same as picking none
        const picked = next.find(client => client.id === $pickedClient);
        if (!picked || picked.active) {
            $pickedClient = null;
        }
    }

    function describe(client: Client): string {
        switch (client.status) {
            case "IN_GAME":
                return client.server ?? "Singleplayer";
            case "CONNECTING":
                return client.server ? `Connecting to ${client.server}` : "Connecting";
            case "DISCONNECTED":
                return "Disconnected";
            default:
                return "In the menu";
        }
    }

    async function toggleExpanded() {
        expanded = !expanded;
        if (!expanded) {
            return;
        }

        accounts = await getAccounts();
        if (!accountNames.includes(accountName)) {
            accountName = accountNames[0] ?? "";
        }
        if (!address.value) {
            address.value = active?.server ?? "";
        }
    }

    function pick(client: Client) {
        $pickedClient = client.active ? null : client.id;
    }

    async function disconnect(client: Client) {
        await closeClient(client.id);
        update(await getClients());
    }

    async function addClient() {
        const account = accounts.find(account => accountLabel(account) === accountName);
        if (!account || adding) {
            return;
        }

        adding = true;
        error = await openClient(account.id, (address.value as string).trim() || undefined);
        adding = false;
        update(await getClients());
    }

    // Views are only drawn while they are open, so they close with the ClickGUI, whose page stays loaded
    function collapse() {
        expanded = false;
    }

    const layer = escapeLayer(collapse);
    $effect(() => layer.setOpen(expanded));

    listen("clientsUpdate", (event: ClientsUpdateEvent) => update(event.clients));

    listen("moduleActivation", (event: ModuleActivationEvent) => {
        if (event.moduleName === "ClickGUI") {
            collapse();
        }
    });

    onMount(async () => {
        update(await getClients());
    });
</script>

<svelte:document onvisibilitychange={() => document.hidden && collapse()}/>

<div class="client-picker">
    {#if shown}
        <button class="current" class:editing={$pickedClient !== null} class:expanded onclick={toggleExpanded}
                type="button">
            <img class="avatar" src={shown.avatar} alt={shown.username}>
            <span class="name">{shown.username}</span>
            <span class="expand-arrow-icon"></span>
        </button>
    {/if}

    {#if expanded}
        <div class="menu" transition:fly={{duration: 200, y: -10, easing: quintOut}}>
            {#each clients as client (client.id)}
                <!-- svelte-ignore a11y_click_events_have_key_events -->
                <!-- svelte-ignore a11y_no_static_element_interactions -->
                <div
                        class="client"
                        class:picked={client === shown}
                        onclick={() => pick(client)}
                >
                    <div class="view">
                        <ClientView {client}/>
                    </div>

                    <div class="info">
                        <span class="name">{client.username}</span>
                        <span class="status">{describe(client)}</span>
                        {#if client.active}
                            <span class="active">Active</span>
                        {/if}
                    </div>

                    <!-- svelte-ignore a11y_click_events_have_key_events -->
                    <!-- svelte-ignore a11y_no_static_element_interactions -->
                    <div class="actions" onclick={event => event.stopPropagation()}>
                        {#if !client.active}
                            <SettingButton value="Switch" on:click={() => activateClient(client.id)}/>
                        {/if}
                        {#if client.id !== MAIN_CLIENT}
                            <SettingButton value="Disconnect" on:click={() => disconnect(client)}/>
                        {/if}
                    </div>
                </div>
            {/each}

            <div class="add">
                {#if accounts.length > 0}
                    <Dropdown name="Account" options={accountNames} bind:value={accountName}/>
                    <TextSetting bind:setting={address}/>
                    <SettingButton value={adding ? "Adding..." : "Add client"} on:click={addClient}/>
                    {#if error}
                        <div class="error">{error}</div>
                    {/if}
                {:else}
                    <div class="hint">Add accounts in the alt manager to play them here.</div>
                {/if}
            </div>
        </div>
    {/if}
</div>

<style lang="scss">
  .client-picker {
    position: fixed;
    top: 15px;
    right: 15px;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    row-gap: 8px;
    z-index: 9999999999;
    user-select: none;
  }

  .current {
    display: flex;
    align-items: center;
    column-gap: 8px;
    padding: 6px 14px 6px 6px;
    border-radius: 999px;
    border: solid 1px transparent;
    background-color: var(--clickgui-tabs-background-color);
    box-shadow: 0 0 10px var(--clickgui-tabs-shadow-color);
    color: var(--clickgui-text-color);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: ease background-color .2s;

    &:hover {
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.editing {
      background-color: var(--clickgui-tab-active-background-color);
      border-color: var(--clickgui-tab-active-border-color);
    }

    .avatar {
      width: 22px;
      height: 22px;
      border-radius: 999px;
      image-rendering: pixelated;
    }
  }

  .expand-arrow-icon {
    width: 11px;
    height: 11px;
    background-image: url("/img/clickgui/icon-settings-expand.svg");
    background-position: center;
    background-repeat: no-repeat;
    opacity: 0.5;
    transform: rotate(-90deg);
    transition: ease opacity .2s, ease transform .4s;
  }

  .current.expanded .expand-arrow-icon {
    transform: rotate(0);
    opacity: 1;
  }

  .menu {
    width: 360px;
    max-height: calc(100vh - 80px);
    overflow: auto;
    border-radius: 5px;
    background-color: var(--clickgui-window-background-color);
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);

    &::-webkit-scrollbar {
      width: 0;
    }
  }

  .client {
    display: grid;
    grid-template-columns: 112px 1fr max-content;
    align-items: center;
    column-gap: 10px;
    padding: 8px 10px;
    border-left: solid 4px transparent;
    cursor: pointer;
    transition: ease background-color .2s;

    &:hover {
      background-color: var(--clickgui-module-hover-background-color);
    }

    &.picked {
      border-left-color: var(--clickgui-module-settings-border-color);
      background-color: var(--clickgui-module-settings-background-color);
    }
  }

  .view {
    border-radius: 3px;
    overflow: hidden;
    background-color: var(--clickgui-input-background-color);
  }

  .info {
    display: flex;
    flex-direction: column;
    row-gap: 2px;
    min-width: 0;
    font-size: 12px;

    .name {
      color: var(--clickgui-text-color);
      font-size: 14px;
      font-weight: 600;
    }

    .status {
      color: var(--clickgui-text-dimmed-color);
    }

    .name, .status {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .active {
      color: var(--clickgui-module-enabled-color);
      font-weight: 600;
    }
  }

  .actions {
    display: flex;
    flex-direction: column;
    row-gap: 5px;
    width: 80px;
  }

  .add {
    display: flex;
    flex-direction: column;
    row-gap: 5px;
    padding: 10px 14px 12px;
    border-top: solid 2px var(--clickgui-window-header-border-color);

    .error {
      color: var(--error-color);
      font-size: 12px;
    }

    .hint {
      color: var(--clickgui-text-dimmed-color);
      font-size: 12px;
    }
  }
</style>
