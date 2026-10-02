<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {LiquidProxySession} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";
    import MenuListItem from "../../common/menulist/MenuListItem.svelte";
    import MenuListItemButton from "../../common/menulist/MenuListItemButton.svelte";
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
            <!-- The country the IP of the session was from -->
            <MenuListItem title={session.username}
                          image={session.country !== "Unknown" ? `img/flags/${session.country.toLowerCase()}.svg` : icon}>
                <svelte:fragment slot="tag">
                    {#if session.type}
                        <MenuListItemTag text={session.type}/>
                    {/if}
                </svelte:fragment>

                <div class="played" slot="subtitle">
                    {#if session.connected}
                        playing {formatDuration(now - session.startedAt)} on
                    {:else}
                        played {formatDuration(session.lastSeenAt - session.startedAt)} on
                    {/if}
                    <img class="server-icon" src={icon} alt="">
                    <span class="server">{session.server}</span>
                </div>

                <svelte:fragment slot="always-visible">
                    {#if session.connected}
                        <MenuListItemButton title="End session" icon="disconnect"
                                            on:click={() => dispatch("end", session.id)}/>
                    {:else if session.error}
                        <span class="error">
                            <ToolTip text={session.error}/>
                            <img src="img/menu/icon-info.svg" alt="error">
                        </span>
                    {/if}
                </svelte:fragment>
            </MenuListItem>
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

  .played {
    display: flex;
    align-items: center;
    column-gap: 6px;

    .server-icon {
      width: 20px;
      height: 20px;
      border-radius: 4px;
    }

    .server {
      color: var(--menu-text-color);
    }
  }

  .error {
    position: relative;
    display: flex;
    margin-left: 15px;

    img {
      height: 27px;
      width: 27px;
    }
  }

  .empty {
    color: var(--menu-text-dimmed-color);
    font-size: 18px;
  }
</style>
