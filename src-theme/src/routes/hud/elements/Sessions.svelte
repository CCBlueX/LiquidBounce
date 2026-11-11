<script lang="ts">
    import {onMount} from "svelte";
    import {fly} from "svelte/transition";
    import {getSessions} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import type {SessionsUpdateEvent} from "../../../integration/events";
    import type {ClientSession, ClientSessionActivity} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import ItemStackView from "./inventory/ItemStackView.svelte";

    const MAX_MODULES = 3;

    const activities: Record<ClientSessionActivity, string> = {
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

    let sessions: ClientSession[] = $state([]);

    // The active session is the one on screen already
    const others = $derived(sessions.filter(session => !session.active));

    function describe(session: ClientSession): string {
        switch (session.status) {
            case "IN_GAME":
                return session.activity ? activities[session.activity] : "";
            case "CONNECTING":
                return "Connecting";
            case "DISCONNECTED":
                return "Disconnected";
            default:
                return "In the menu";
        }
    }

    listen("sessionsUpdate", (event: SessionsUpdateEvent) => {
        sessions = event.sessions;
    });

    onMount(async () => {
        sessions = await getSessions();
    });
</script>

{#if others.length > 0}
    <div class="sessions">
        {#each others as session (session.id)}
            <div class="session" transition:fly={{ x: 10, duration: 200 }}>
                <img class="avatar" src={session.avatar} alt="avatar">

                <div class="name">{session.username}</div>
                <div class="state" class:offline={session.status === "DISCONNECTED"}>
                    {describe(session)}{#if session.server}<span class="server">{session.server}</span>{/if}
                </div>

                {#if session.status === "IN_GAME"}
                    <div class="stats">
                        <div class="stat">
                            <span class="value">{Math.ceil(session.health ?? 0)}</span>
                            <img class="icon" src="img/hud/targethud/icon-health.svg" alt="health">
                        </div>
                        <div class="stat">
                            <span class="value">{session.food ?? 0}</span>
                            <img class="icon" src="img/hud/hotbar/icon-food.svg" alt="food">
                        </div>
                    </div>

                    {#if session.heldItem}
                        <div class="held-item">
                            {#key `${session.heldItem.identifier}:${session.heldItem.count}:${session.heldItem.damage}`}
                                <ItemStackView stack={session.heldItem}/>
                            {/key}
                        </div>
                    {/if}

                    {#if session.modules.length > 0}
                        <div class="modules">
                            {#each session.modules.slice(0, MAX_MODULES) as module (module)}
                                <span class="module">{$spaceSeperatedNames ? convertToSpacedString(module) : module}</span>
                            {/each}
                            {#if session.modules.length > MAX_MODULES}
                                <span class="more">+{session.modules.length - MAX_MODULES}</span>
                            {/if}
                        </div>
                    {/if}
                {/if}

                {#if session.lastMessage}
                    <div class="message">
                        <TextComponent textComponent={session.lastMessage} fontSize={12}/>
                    </div>
                {/if}
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">

  .sessions {
    display: flex;
    flex-direction: column;
    gap: 6px;
    width: 230px;
  }

  .session {
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
    background-color: var(--sessions-background-color);
    color: var(--sessions-text-color);
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
    color: var(--sessions-text-dimmed-color);
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &.offline {
      color: var(--sessions-health-color);
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
      color: var(--sessions-text-dimmed-color);
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
      color: var(--sessions-module-color);
      font-weight: 500;
    }

    .more {
      color: var(--sessions-text-dimmed-color);
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
