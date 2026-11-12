<script lang="ts">
    import {onMount} from "svelte";
    import {fly} from "svelte/transition";
    import {getClients} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import type {ClientsUpdateEvent} from "../../../integration/events";
    import type {Client} from "../../../integration/types";
    import ClientView from "../../../components/ClientView.svelte";

    // The space the views may take, in HUD pixels, which follow the GUI scale
    const MAX_WIDTH = 320;
    const MAX_TOTAL_WIDTH = 400;
    const MAX_TOTAL_HEIGHT = 340;
    const GAP = 8;

    let clients: Client[] = $state([]);

    // The active client is the one on screen already
    const others = $derived(clients.filter(client => !client.active));
    const layout = $derived(fit(others.length));

    /**
     * As large as possible within the space, in as many columns as make the views clearly larger.
     */
    function fit(count: number): { columns: number, width: number } {
        let best = {columns: 1, width: 0};
        for (let columns = 1; columns <= count; columns++) {
            const rows = Math.ceil(count / columns);
            const byHeight = (MAX_TOTAL_HEIGHT - (rows - 1) * GAP) / rows * 16 / 9;
            const byWidth = (MAX_TOTAL_WIDTH - (columns - 1) * GAP) / columns;
            const width = Math.floor(Math.min(MAX_WIDTH, byHeight, byWidth));
            if (width > best.width * 1.1) {
                best = {columns, width};
            }
        }
        return best;
    }

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
    <div class="clients" style:--columns={layout.columns} style:--width="{layout.width}px">
        {#each others as client (client.id)}
            <div class="client" class:small={layout.width < 200} transition:fly={{ x: 10, duration: 200 }}>
                <ClientView {client}/>

                <div class="overlay">
                    <span class="name">{client.username}</span>
                    {#if client.status === "IN_GAME"}
                        <span class="health-value">
                            {Math.ceil(client.health ?? 0)}
                            <img class="heart" src="img/hud/targethud/icon-health.svg" alt="health">
                        </span>
                    {:else}
                        <span class="state" class:offline={client.status === "DISCONNECTED"}>{describe(client)}</span>
                    {/if}
                </div>

                {#if client.status === "IN_GAME"}
                    <div class="health" style:width="{health(client)}%"></div>
                {/if}
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">
  .clients {
    display: grid;
    grid-template-columns: repeat(var(--columns), var(--width));
    gap: 8px;
  }

  .client {
    position: relative;
    border-radius: 8px;
    overflow: hidden;
    background-color: var(--clients-background-color);
    box-shadow: 0 0 0 1px color-mix(in srgb, white 10%, transparent), 0 4px 14px rgba(0, 0, 0, 0.35);
  }

  .overlay {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    column-gap: 8px;
    padding: 24px 10px 8px;
    background: linear-gradient(transparent, color-mix(in srgb, black 70%, transparent));
    font-size: 14px;
    text-shadow: 0 1px 3px black;

    .small & {
      padding: 16px 7px 6px;
      font-size: 12px;
    }
  }

  .name {
    color: var(--clients-text-color);
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .health-value {
    display: flex;
    align-items: center;
    column-gap: 4px;
    color: var(--clients-text-color);
    font-weight: 600;
    white-space: nowrap;

    .heart {
      width: 12px;
      height: 12px;
    }
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
    height: 3px;
    background-color: var(--clients-health-color);
    transition: width .2s ease;
  }
</style>
