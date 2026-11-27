<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {LiquidProxySession} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";
    import MenuListItem from "../../common/menulist/MenuListItem.svelte";
    import MenuListItemButton from "../../common/menulist/MenuListItemButton.svelte";
    import MenuListItemTag from "../../common/menulist/MenuListItemTag.svelte";

    export let sessions: LiquidProxySession[] = [];
    // Ticks so that running sessions count up
    export let now = Date.now();

    const UNKNOWN_SERVER_ICON = `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`;
    const IP_TYPES = {hosted: "Hosted", isp: "ISP", residential: "Residential", mobile: "Mobile"};

    const dispatch = createEventDispatcher<{ end: string, changeIp: string }>();

    function formatDuration(millis: number): string {
        const minutes = Math.round(millis / 60_000);
        if (minutes < 1) {
            return "less than a minute";
        }
        if (minutes < 60) {
            return minutes === 1 ? "1 minute" : `${minutes} minutes`;
        }

        const hours = Math.floor(minutes / 60);
        const rest = minutes % 60;
        const hoursText = hours === 1 ? "1 hour" : `${hours} hours`;
        return rest === 0 ? hoursText : `${hoursText} ${rest} min`;
    }
</script>

<div class="session-history">
    <div class="title">Session History</div>

    <div class="sessions">
        {#each sessions as session (session.id)}
            {@const icon = session.icon ?? UNKNOWN_SERVER_ICON}
            <!-- Leads with the country the IP of the session is from -->
            <MenuListItem title={session.player}
                          image={session.country ? `img/flags/${session.country.toLowerCase()}.svg` : icon}>
                <svelte:fragment slot="tag">
                    {#if session.ipType}
                        <MenuListItemTag text={IP_TYPES[session.ipType]}/>
                    {/if}
                </svelte:fragment>

                <div class="played" slot="subtitle">
                    {session.connected ? "playing" : "played"}
                    {formatDuration((session.endedAt ?? now) - session.startedAt)} on
                    <img class="server-icon" src={icon} alt="">
                    <span class="server">{session.server}</span>
                </div>

                <svelte:fragment slot="always-visible">
                    {#if session.connected}
                        <MenuListItemButton title="End session" icon="disconnect"
                                            on:click={() => dispatch("end", session.id)}/>
                    {/if}
                    {#if session.canChangeIp}
                        <MenuListItemButton title="New IP on next join" icon="refresh"
                                            on:click={() => dispatch("changeIp", session.id)}/>
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

  .empty {
    color: var(--menu-text-dimmed-color);
    font-size: 18px;
  }
</style>
