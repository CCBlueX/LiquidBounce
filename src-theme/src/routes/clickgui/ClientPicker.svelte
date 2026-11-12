<script lang="ts">
    import {onMount} from "svelte";
    import {fade, scale} from "svelte/transition";
    import {cubicOut} from "svelte/easing";
    import type {Account, Client, Server} from "../../integration/types";
    import {activateClient, closeClient, getAccounts, getClients, getServers, openClient} from "../../integration/rest";
    import {REST_BASE} from "../../integration/host";
    import {listen} from "../../integration/ws";
    import type {ClientsUpdateEvent, ModuleActivationEvent} from "../../integration/events";
    import {escapeLayer} from "../../integration/escape";
    import {pickedClient} from "./clickgui_store";
    import ClientView from "../../components/ClientView.svelte";

    const MAIN_CLIENT = 0;
    const UNKNOWN_SERVER_ICON = `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`;

    let clients = $state<Client[]>([]);
    let accounts = $state<Account[]>([]);
    let expanded = $state(false);

    let formOpen = $state(false);
    let openList = $state<"account" | "server" | null>(null);
    let accountId = $state<number | null>(null);
    let servers = $state<Server[]>([]);
    let address = $state<string | null>(null);
    let adding = $state(false);
    let error = $state<string | null>(null);

    const active = $derived(clients.find(client => client.active));
    // The client on screen first, it needs no view since the window shows it
    const ordered = $derived([...clients].sort((a, b) => Number(b.active) - Number(a.active)));
    const shown = $derived(clients.find(client => client.id === $pickedClient) ?? active);
    const account = $derived(accounts.find(account => account.id === accountId));

    // The server the window plays on comes first, then the server list
    const currentServer = $derived(active?.status === "IN_GAME" ? active.server ?? null : null);
    const serverChoices = $derived([
        ...(currentServer ? [{name: "Current server", address: currentServer, icon: iconOf(currentServer)}] : []),
        ...servers.map(server => ({name: server.name, address: server.address, icon: iconOf(server.address)}))
    ]);
    const serverChoice = $derived(serverChoices.find(choice => choice.address === address));

    function iconOf(serverAddress: string): string {
        const icon = servers.find(server => server.address === serverAddress)?.icon;
        return icon ? `data:image/png;base64,${icon}` : UNKNOWN_SERVER_ICON;
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
        if (!account) {
            accountId = accounts[0]?.id ?? null;
        }
    }

    // The server list is asked for only here, since asking pings every server on it
    async function openForm() {
        formOpen = true;
        servers = await getServers().catch(() => []);
        if (!serverChoice) {
            address = serverChoices[0]?.address ?? null;
        }
    }

    function toggleList(list: "account" | "server") {
        openList = openList === list ? null : list;
    }

    function pick(client: Client) {
        $pickedClient = client.active ? null : client.id;
    }

    function pickOnKey(event: KeyboardEvent, client: Client) {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            pick(client);
        }
    }

    async function switchTo(event: MouseEvent, client: Client) {
        event.stopPropagation();
        await activateClient(client.id);
    }

    async function disconnect(event: MouseEvent, client: Client) {
        event.stopPropagation();
        await closeClient(client.id);
        update(await getClients());
    }

    function chooseAccount(next: Account) {
        accountId = next.id;
        openList = null;
    }

    function chooseServer(next: string) {
        address = next;
        openList = null;
    }

    async function addClient() {
        if (accountId === null || adding) {
            return;
        }

        adding = true;
        error = await openClient(accountId, address ?? undefined);
        adding = false;
        if (!error) {
            formOpen = false;
        }
        update(await getClients());
    }

    // Views are only drawn while they are open, so they close with the ClickGUI, whose page stays loaded
    function collapse() {
        expanded = false;
        openList = null;
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

{#snippet entry(icon: string, name: string, detail: string)}
    <img class="avatar" src={icon} alt="">
    <span class="entry-name">{name}</span>
    <span class="entry-detail">{detail}</span>
{/snippet}

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
        <div class="menu" in:scale={{start: 0.96, duration: 160, easing: cubicOut}} out:fade={{duration: 100}}>
            <div class="clients">
                {#each ordered as client (client.id)}
                    <div class="client" class:compact={client.active} class:picked={client === shown} role="button"
                         tabindex="0" onclick={() => pick(client)} onkeydown={event => pickOnKey(event, client)}>
                        {#if client.active}
                            <img class="head" src={client.avatar} alt="">
                        {:else}
                            <div class="view">
                                <ClientView {client}/>
                            </div>
                        {/if}

                        <div class="info">
                            <span class="name">{client.username}</span>
                            <span class="status">{describe(client)}</span>
                        </div>

                        <div class="actions">
                            {#if !client.active}
                                <button class="icon-button" type="button" title="Switch to {client.username}"
                                        onclick={event => switchTo(event, client)}>
                                    <img src="img/menu/icon-play.svg" alt="Switch">
                                </button>
                            {/if}
                            {#if client.id !== MAIN_CLIENT}
                                <button class="icon-button danger" type="button" title="Disconnect"
                                        onclick={event => disconnect(event, client)}>
                                    <img src="img/menu/icon-disconnect.svg" alt="Disconnect">
                                </button>
                            {/if}
                        </div>
                    </div>
                {/each}
            </div>

            <div class="divider"></div>

            {#if !formOpen}
                <button class="add-toggle" type="button" onclick={openForm}>
                    <img src="img/menu/icon-plus-circle.svg" alt="">
                    Add client
                </button>
            {:else if accounts.length === 0}
                <div class="hint">Add accounts in the alt manager to play them here.</div>
            {:else}
                <div class="add">
                    <button class="field select" class:open={openList === "account"} type="button"
                            onclick={() => toggleList("account")}>
                        {#if account}
                            {@render entry(account.avatar, account.username, account.type)}
                        {/if}
                        <span class="expand-arrow-icon"></span>
                    </button>

                    {#if openList === "account"}
                        <div class="options">
                            {#each accounts as option (option.id)}
                                <button class="option" class:chosen={option.id === accountId} type="button"
                                        onclick={() => chooseAccount(option)}>
                                    {@render entry(option.avatar, option.username, option.type)}
                                </button>
                            {/each}
                        </div>
                    {/if}

                    <button class="field select" class:open={openList === "server"} type="button"
                            disabled={serverChoices.length === 0} onclick={() => toggleList("server")}>
                        {#if serverChoice}
                            {@render entry(serverChoice.icon, serverChoice.name, serverChoice.address)}
                        {:else}
                            <span class="placeholder">No server</span>
                        {/if}
                        <span class="expand-arrow-icon"></span>
                    </button>

                    {#if openList === "server"}
                        <div class="options">
                            {#each serverChoices as choice}
                                <button class="option" class:chosen={choice.address === address} type="button"
                                        onclick={() => chooseServer(choice.address)}>
                                    {@render entry(choice.icon, choice.name, choice.address)}
                                </button>
                            {/each}
                        </div>
                    {/if}

                    <button class="primary" type="button" disabled={adding || accountId === null} onclick={addClient}>
                        {adding ? "Adding" : "Add client"}
                    </button>

                    {#if error}
                        <div class="error">{error}</div>
                    {/if}
                </div>
            {/if}
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
    transition: background-color .2s ease, transform 150ms ease-out;

    &:hover {
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &:active {
      transform: scale(0.97);
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
    flex-shrink: 0;
    background-image: url("/img/clickgui/icon-settings-expand.svg");
    background-position: center;
    background-repeat: no-repeat;
    opacity: 0.5;
    transform: rotate(-90deg);
    transition: opacity .2s ease, transform .4s ease;
  }

  .current.expanded .expand-arrow-icon,
  .select.open .expand-arrow-icon {
    transform: rotate(0);
    opacity: 1;
  }

  .menu {
    width: 340px;
    padding: 6px;
    border-radius: 18px;
    background-color: var(--clickgui-tabs-background-color);
    box-shadow: 0 0 10px var(--clickgui-tabs-shadow-color);
    transform-origin: top right;
  }

  .clients {
    display: flex;
    flex-direction: column;
    row-gap: 2px;
    max-height: calc(100vh - 220px);
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 0;
    }
  }

  .client {
    display: grid;
    grid-template-columns: 120px 1fr max-content;
    align-items: center;
    column-gap: 12px;
    padding: 6px;
    border-radius: 12px;
    border: solid 1px transparent;
    cursor: pointer;
    transition: background-color 150ms ease;

    &:hover {
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.picked {
      background-color: var(--clickgui-tab-active-background-color);
      border-color: var(--clickgui-tab-active-border-color);
    }

    &:focus-visible {
      outline: solid 2px var(--clickgui-tab-active-border-color);
      outline-offset: 1px;
    }

    &.compact {
      grid-template-columns: 28px 1fr max-content;
      padding: 8px 10px;
    }
  }

  .head {
    width: 28px;
    height: 28px;
    border-radius: 999px;
    image-rendering: pixelated;
  }

  .view {
    border-radius: 8px;
    overflow: hidden;
    background-color: color-mix(in srgb, var(--clickgui-text-color) 6%, transparent);
  }

  .info {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    row-gap: 2px;
    min-width: 0;

    .name {
      color: var(--clickgui-text-color);
      font-size: 14px;
      font-weight: 600;
    }

    .status {
      color: var(--clickgui-text-dimmed-color);
      font-size: 12px;
    }

    .name, .status {
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .actions {
    display: flex;
    column-gap: 2px;
  }

  .icon-button {
    display: grid;
    place-items: center;
    width: 30px;
    height: 30px;
    border: none;
    border-radius: 999px;
    background-color: transparent;
    opacity: 0.75;
    cursor: pointer;
    transition: background-color 150ms ease, opacity 150ms ease, transform 150ms ease-out;

    img {
      width: 15px;
      height: 15px;
    }

    &:hover,
    &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.danger:hover {
      background-color: color-mix(in srgb, var(--error-color) 25%, transparent);
    }

    &:active {
      transform: scale(0.92);
    }
  }

  .divider {
    height: 1px;
    margin: 6px 10px;
    background-color: var(--clickgui-global-settings-divider-color);
  }

  .add-toggle {
    display: flex;
    align-items: center;
    column-gap: 10px;
    width: 100%;
    padding: 10px 12px;
    border: none;
    border-radius: 12px;
    background-color: transparent;
    color: var(--clickgui-text-dimmed-color);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 150ms ease, color 150ms ease;

    img {
      width: 18px;
      height: 18px;
      opacity: 0.6;
      transition: opacity 150ms ease;
    }

    &:hover {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-tab-hover-background-color);

      img {
        opacity: 1;
      }
    }
  }

  .hint {
    padding: 8px 12px 10px;
    color: var(--clickgui-text-dimmed-color);
    font-size: 12px;
  }

  .add {
    display: flex;
    flex-direction: column;
    row-gap: 6px;
    padding: 4px 6px 6px;
  }

  .field {
    height: 36px;
    padding: 0 12px;
    border-radius: 10px;
    border: solid 1px transparent;
    background-color: color-mix(in srgb, var(--clickgui-text-color) 6%, transparent);
    color: var(--clickgui-text-color);
    font-size: 13px;
    transition: border-color 150ms ease, background-color 150ms ease;

    &:hover {
      background-color: color-mix(in srgb, var(--clickgui-text-color) 9%, transparent);
    }
  }

  .select {
    display: flex;
    align-items: center;
    column-gap: 10px;
    padding-left: 8px;
    cursor: pointer;

    &.open {
      border-color: var(--clickgui-tab-active-border-color);
    }

    &:disabled {
      cursor: default;
    }

    .expand-arrow-icon {
      margin-left: auto;
    }
  }

  .avatar {
    width: 20px;
    height: 20px;
    border-radius: 5px;
    image-rendering: pixelated;
  }

  .entry-name {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-weight: 600;
  }

  .entry-detail {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--clickgui-text-dimmed-color);
    font-size: 11px;
  }

  .placeholder {
    color: var(--clickgui-text-dimmed-color);
  }

  .options {
    display: flex;
    flex-direction: column;
    max-height: 180px;
    padding: 4px;
    overflow-y: auto;
    border-radius: 10px;
    background-color: color-mix(in srgb, var(--clickgui-text-color) 4%, transparent);

    &::-webkit-scrollbar {
      width: 0;
    }
  }

  .option {
    display: flex;
    align-items: center;
    column-gap: 10px;
    padding: 6px 8px;
    border: none;
    border-radius: 8px;
    background-color: transparent;
    color: var(--clickgui-text-dimmed-color);
    font-size: 13px;
    text-align: left;
    cursor: pointer;
    transition: background-color 150ms ease, color 150ms ease;

    &:hover {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.chosen {
      color: var(--clickgui-text-color);
    }
  }

  .primary {
    height: 36px;
    margin-top: 2px;
    padding: 0 18px;
    border: none;
    border-radius: 10px;
    background-color: var(--clickgui-button-background-color);
    color: var(--clickgui-text-color);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 150ms ease, transform 150ms ease-out;

    &:hover {
      background-color: var(--clickgui-button-hover-background-color);
    }

    &:active {
      transform: scale(0.97);
    }

    &:disabled {
      opacity: 0.6;
      cursor: default;
    }
  }

  .error {
    padding: 0 4px;
    color: var(--error-color);
    font-size: 12px;
  }
</style>
