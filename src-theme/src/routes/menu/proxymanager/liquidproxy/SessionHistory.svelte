<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {LiquidProxyLocation, LiquidProxySession} from "../../../../integration/types";
    import MenuListItemTag from "../../common/menulist/MenuListItemTag.svelte";
    import ToolTip from "../../common/ToolTip.svelte";
    import {formatDuration, locationLabel} from "./liquidproxy";

    export let sessions: LiquidProxySession[] = [];
    export let locations: LiquidProxyLocation[] = [];
    // Ticks so that running sessions count up
    export let now = Date.now();

    const dispatch = createEventDispatcher<{ end: string }>();

    function location(session: LiquidProxySession) {
        const location = locations.find(l => l.code === session.location);
        return location ? locationLabel(location, locations) : null;
    }

    function flag(session: LiquidProxySession) {
        return session.country === "Unknown" ? null : `img/flags/${session.country.toLowerCase()}.svg`;
    }
</script>

<div class="session-history">
    <div class="title">Session History</div>

    <div class="sessions">
        {#each sessions as session (session.id)}
            {@const image = flag(session)}
            {@const through = location(session)}
            <div class="session" class:active={session.connected}>
                {#if image}
                    <img class="flag" src={image} alt={session.country}>
                {:else}
                    <div class="flag"></div>
                {/if}

                <div class="name">
                    <img class="head" src={session.avatar} alt={session.username}>
                    <span>{session.username}</span>
                    {#if session.type}
                        <MenuListItemTag text={session.type}/>
                    {/if}
                    {#if through}
                        <MenuListItemTag text={through}/>
                    {/if}
                </div>

                <div class="played">
                    {#if session.connected}
                        playing {formatDuration(now - session.startedAt)} on
                    {:else}
                        played {formatDuration(session.lastSeenAt - session.startedAt)} on
                    {/if}
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

  .flag {
    grid-area: a;
    width: 68px;
    height: 68px;
    border-radius: 50%;
    background-color: var(--menu-base-68-color);
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
    color: var(--menu-text-dimmed-color);
    font-size: 18px;

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
