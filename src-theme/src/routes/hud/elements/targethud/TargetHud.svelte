<script lang="ts">
    import ArmorStatus from "./ArmorStatus.svelte";
    import {listen} from "../../../../integration/ws.js";
    import type {PlayerStats} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";

    let target: PlayerStats | null = null;

    listen("targetChange", (data: any) => {
        console.log(JSON.stringify(data));

        target = data.target;
    });
</script>

{#if target != null}
    <div class="targethud">
        <div class="avatar">
            <img src="{REST_BASE}/api/v1/client/resource?id={target.skinIdentifier}" alt="avatar" />
        </div>

        <div class="name">{target.username}</div>
        <div class="health-stats">
            <div class="stat">
                <span class="value">{target.health}</span>
                <img
                        class="icon"
                        src="img/hud/targethud/icon-health.svg"
                        alt="health"
                />
            </div>
            <div class="stat">
                <span class="value">{target.armor}</span>
                <img
                        class="icon"
                        src="img/hud/targethud/icon-armor.svg"
                        alt="armor"
                />
            </div>
        </div>
        <div class="armor-stats">
            <ArmorStatus durability={5} />
            <ArmorStatus durability={5} />
            <ArmorStatus durability={5} />
            <ArmorStatus durability={5} />
        </div>
    </div>
{/if}

<style lang="scss">
    @import "../../../../colors.scss";

    .targethud {
        position: fixed;
        top: 50%;
        left: calc(50% + 20px);
        transform: translateY(-50%);
        background-color: rgba($targethud-base-color, 0.68);
        padding: 10px 15px;
        border-radius: 5px;
        display: grid;
        grid-template-areas:
            "a b d"
            "a c d";
        column-gap: 10px;
    }

    .name {
        grid-area: b;
        color: $targethud-text-color;
        font-weight: 500;
        align-self: flex-end;
    }

    .health-stats {
        grid-area: c;
        display: flex;
        column-gap: 10px;

        .stat {
            .value {
                color: $targethud-text-dimmed-color;
                font-size: 14px;
            }
        }
    }

    .armor-stats {
        grid-area: d;
        display: flex;
        align-items: center;
        column-gap: 10px;
        padding-left: 5px;
    }

    .avatar {
        grid-area: a;
        height: 50px;
        width: 50px;
        position: relative;
        image-rendering: pixelated;
        background-image: url("/img/hud/targethud/steve.png");
        background-repeat: no-repeat;
        background-size: cover;
        border-radius: 5px;
        overflow: hidden;

        img {
            height: 100%;
            width: 100%;
        }
    }
</style>
