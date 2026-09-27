<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {LiquidProxySession} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";
    import MenuListItemTag from "../../common/menulist/MenuListItemTag.svelte";
    import ToolTip from "../../common/ToolTip.svelte";
    import {formatDuration} from "./liquidproxy";

    export let sessions: LiquidProxySession[] = [];
    // Ticks so that running sessions count up
    export let now = Date.now();

    const UNKNOWN_SERVER_ICON = `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`;

    const dispatch = createEventDispatcher<{ end: string }>();
</script>

<div class="session-history">
    <div class="title">Session History</div>

    <div class="sessions">
        {#each sessions as session (session.id)}
            {@const icon = session.icon ?? UNKNOWN_SERVER_ICON}
            <div class="session" class:active={session.connected}>
                <!-- The country the IP of the session was from -->
                {#if session.country !== "Unknown"}
                    <img class="image" src="img/flags/{session.country.toLowerCase()}.svg" alt={session.country}>
                {:else}
                    <img class="image" src={icon} alt={session.server}>
                {/if}

                <div class="name">
                    <img class="head" src={session.avatar} alt={session.username}>
                    <span>{session.username}</span>
                    {#if session.type}
                        <MenuListItemTag text={session.type}/>
                    {/if}
                </div>

                <div class="played">
                    {#if session.connected}
                        playing {formatDuration(now - session.startedAt)} on
                    {:else}
                        played {formatDuration(session.lastSeenAt - session.startedAt)} on
                    {/if}
                    <img class="server-icon" src={icon} alt="">
                    <span class="server">{session.server}</span>
                </div>

                <div class="action">
                    {#if session.connected}
                        <button on:click={() => dispatch("end", session.id)}>
                            <ToolTip text="End session"/>
                            <img src="img/menu/liquidproxy/power.svg" alt="end session">
                        </button>
                    {:else if session.error}
                        <span>
                            <ToolTip text={session.error}/>
                            <img src="img/menu/liquidproxy/triangle-alert.svg" alt="error">
                        </span>
                    {/if}
                </div>
            </div>
        {:else}
            <div class="empty">Sessions you play through LiquidProxy show up here.</div>
        {/each}
    </div>
</div>

<style lang="scss">
  .session-history {
    display: flex;
    flex-direction: column;
    row-gap: 20px;
    min-height: 0;
  }

  .title {
    color: var(--menu-text-color);
    font-size: 20px;
    font-weight: 600;
  }

  .sessions {
    display: flex;
    flex-direction: column;
    row-gap: 15px;
    overflow: auto;
    min-height: 0;
  }

  .session {
    display: grid;
    grid-template-areas:
        "a b c"
        "a d c";
    grid-template-columns: max-content 1fr max-content;
    column-gap: 15px;
    align-items: center;
    background-color: var(--menu-list-item-background-color);
    padding: 15px 25px;
    border-radius: 5px;
    transition: ease background-color .2s;

    :global(.tag) {
      background-color: var(--accent-color);
    }

    &.active {
      background-color: var(--accent-color);

      .played {
        color: var(--menu-text-color);
      }

      :global(.tag) {
        background-color: var(--menu-base-80-color);
      }
    }
  }

  .image {
    grid-area: a;
    display: block;
    width: 68px;
    height: 68px;
    border-radius: 50%;
    object-fit: cover;
  }

  .name {
    grid-area: b;
    align-self: flex-end;
    display: flex;
    align-items: center;
    color: var(--menu-text-color);
    font-size: 20px;
    font-weight: 600;

    .head {
      width: 22px;
      height: 22px;
      margin-right: 8px;
      image-rendering: pixelated;
      border-radius: 3px;
    }
  }

  .played {
    grid-area: d;
    align-self: flex-start;
    display: flex;
    align-items: center;
    column-gap: 6px;
    color: var(--menu-text-dimmed-color);
    font-size: 18px;

    .server-icon {
      width: 20px;
      height: 20px;
      border-radius: 4px;
    }

    .server {
      color: var(--menu-text-color);
    }
  }

  .action {
    grid-area: c;

    button, span {
      display: flex;
      background: none;
      border: none;
      padding: 0;
      position: relative;
    }

    button {
      cursor: pointer;
    }

    img {
      height: 27px;
    }
  }

  .empty {
    color: var(--menu-text-dimmed-color);
    font-size: 18px;
  }
</style>
