<script lang="ts">
    import {onMount} from "svelte";
    import {fly} from "svelte/transition";
    import {getClients} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import type {ClientsUpdateEvent} from "../../../integration/events";
    import type {Client, ClientActivity} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import ItemStackView from "./inventory/ItemStackView.svelte";

    const MAX_MODULES = 3;

    const activities: Record<ClientActivity, string> = {
        DEAD: "Dead",
        FIGHTING: "Fighting",
        MINING: "Mining",
        USING_ITEM: "Using an item",
        IN_CONTAINER: "In a container",
        SLEEPING: "Sleeping",
        RIDING: "Riding",
        GLIDING: "Gliding",
        SWIMMING: "Swimming",
        MOVING: "Moving",
        IDLE: "Idle"
    };

    let clients: Client[] = $state([]);

    // The active client is the one on screen already
    const others = $derived(clients.filter(client => !client.active));

    function describe(client: Client): string {
        switch (client.status) {
            case "IN_GAME":
                return client.activity ? activities[client.activity] : "";
            case "CONNECTING":
                return "Connecting";
            case "DISCONNECTED":
                return "Disconnected";
            default:
                return "In the menu";
        }
    }

    listen("clientsUpdate", (event: ClientsUpdateEvent) => {
        clients = event.clients;
    });

    onMount(async () => {
        clients = await getClients();
    });
</script>

{#if others.length > 0}
    <div class="clients">
        {#each others as client (client.id)}
            <div class="client" transition:fly={{ x: 10, duration: 200 }}>
                <img class="avatar" src={client.avatar} alt="avatar">

                <div class="name">{client.username}</div>
                <div class="state" class:offline={client.status === "DISCONNECTED"}>
                    {describe(client)}{#if client.server}<span class="server">{client.server}</span>{/if}
                </div>

                {#if client.status === "IN_GAME"}
                    <div class="stats">
                        <div class="stat">
                            <span class="value">{Math.ceil(client.health ?? 0)}</span>
                            <img class="icon" src="img/hud/targethud/icon-health.svg" alt="health">
                        </div>
                        <div class="stat">
                            <span class="value">{client.food ?? 0}</span>
                            <img class="icon" src="img/hud/hotbar/icon-food.svg" alt="food">
                        </div>
                    </div>

                    {#if client.heldItem}
                        <div class="held-item">
                            {#key `${client.heldItem.identifier}:${client.heldItem.count}:${client.heldItem.damage}`}
                                <ItemStackView stack={client.heldItem}/>
                            {/key}
                        </div>
                    {/if}

                    {#if client.modules.length > 0}
                        <div class="modules">
                            {#each client.modules.slice(0, MAX_MODULES) as module (module)}
                                <span class="module">{$spaceSeperatedNames ? convertToSpacedString(module) : module}</span>
                            {/each}
                            {#if client.modules.length > MAX_MODULES}
                                <span class="more">+{client.modules.length - MAX_MODULES}</span>
                            {/if}
                        </div>
                    {/if}
                {/if}

                {#if client.lastMessage}
                    <div class="message">
                        <TextComponent textComponent={client.lastMessage} fontSize={12}/>
                    </div>
                {/if}
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">

  .clients {
    display: flex;
    flex-direction: column;
    gap: 6px;
    width: 230px;
  }

  .client {
    display: grid;
    grid-template-areas:
      "avatar name stats item"
      "avatar state state item"
      "modules modules modules modules"
      "message message message message";
    grid-template-columns: max-content 1fr max-content max-content;
    column-gap: 10px;
    row-gap: 2px;
    align-items: center;
    padding: 8px 12px;
    border-radius: 5px;
    background-color: var(--clients-background-color);
    color: var(--clients-text-color);
    font-size: 14px;
  }

  .avatar {
    grid-area: avatar;
    width: 32px;
    height: 32px;
    border-radius: 5px;
    image-rendering: pixelated;
  }

  .name {
    grid-area: name;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .state {
    grid-area: state;
    color: var(--clients-text-dimmed-color);
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &.offline {
      color: var(--clients-health-color);
    }

    .server::before {
      content: "·";
      margin: 0 4px;
    }
  }

  .stats {
    grid-area: stats;
    display: flex;
    column-gap: 8px;

    .stat {
      display: flex;
      align-items: center;
      column-gap: 3px;
    }

    .value {
      color: var(--clients-text-dimmed-color);
      font-size: 12px;
    }

    .icon {
      width: 12px;
      height: 12px;
    }
  }

  .held-item {
    grid-area: item;
    scale: 0.75;
  }

  .modules {
    grid-area: modules;
    display: flex;
    flex-wrap: wrap;
    column-gap: 8px;
    margin-top: 4px;
    font-size: 12px;

    .module {
      color: var(--clients-module-color);
      font-weight: 500;
    }

    .more {
      color: var(--clients-text-dimmed-color);
    }
  }

  .message {
    grid-area: message;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    opacity: 0.8;
  }
</style>
