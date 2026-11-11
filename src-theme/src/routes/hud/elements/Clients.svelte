<script lang="ts">
    import {onMount} from "svelte";
    import {fly} from "svelte/transition";
    import {getClients} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import type {ClientsUpdateEvent} from "../../../integration/events";
    import type {Client} from "../../../integration/types";
    import ClientView from "../../../components/ClientView.svelte";

    let clients: Client[] = $state([]);

    // The active client is the one on screen already
    const others = $derived(clients.filter(client => !client.active));

    function health(client: Client): number {
        return Math.min(1, (client.health ?? 0) / (client.maxHealth || 20)) * 100;
    }

    function describe(client: Client): string {
        switch (client.status) {
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
                <ClientView {client}/>

                <div class="overlay">
                    <span class="name">{client.username}</span>
                    {#if client.status !== "IN_GAME"}
                        <span class="state" class:offline={client.status === "DISCONNECTED"}>{describe(client)}</span>
                    {/if}
                </div>

                {#if client.status === "IN_GAME"}
                    <div class="health" style="width: {health(client)}%"></div>
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
    width: 160px;
  }

  .client {
    position: relative;
    border-radius: 5px;
    overflow: hidden;
    background-color: var(--clients-background-color);
  }

  .overlay {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    column-gap: 6px;
    padding: 12px 6px 5px;
    background: linear-gradient(transparent, var(--clients-background-color));
    font-size: 12px;
    text-shadow: 0 0 3px black;
  }

  .name {
    color: var(--clients-text-color);
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .state {
    color: var(--clients-text-dimmed-color);
    white-space: nowrap;

    &.offline {
      color: var(--clients-health-color);
    }
  }

  .health {
    position: absolute;
    left: 0;
    bottom: 0;
    height: 2px;
    background-color: var(--clients-health-color);
    transition: ease width .2s;
  }
</style>
