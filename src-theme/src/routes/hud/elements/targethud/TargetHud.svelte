<script lang="ts">
    import ArmorStatus from "./ArmorStatus.svelte";
    import {listen} from "../../../../integration/ws.js";
    import type {PlayerData, Vec3} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";
    import {fly} from "svelte/transition";
    import HealthProgress from "./HealthProgress.svelte";
    import type {ClientPlayerDataEvent, TargetChangeEvent} from "../../../../integration/events";
    import {itemTextureUrl} from "../../../../integration/rest";

    let target: PlayerData | null = null;
    let visible = true;
    let ownPosition: Vec3 | null = null;
    let distance: number | null = null;
    let hurtFlash: HTMLElement | null = null;

    let hideTimeout: number;

    function startHideTimeout() {
        hideTimeout = setTimeout(() => {
            visible = false;
        }, 1000);
    }

    function flashIfHurt(previous: PlayerData | null, next: PlayerData | null) {
        if (!previous || !next || previous.uuid !== next.uuid ||
            next.actualHealth + next.absorption >= previous.actualHealth + previous.absorption) {
            return;
        }

        hurtFlash?.animate([{opacity: 1}, {opacity: 0}], {duration: 350, easing: "ease-out"});
    }

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        ownPosition = event.playerData.position;
    });

    listen("targetChange", (data: TargetChangeEvent) => {
        flashIfHurt(target, data.target);
        target = data.target;
        distance = target && ownPosition ? Math.hypot(
            target.position.x - ownPosition.x,
            target.position.y - ownPosition.y,
            target.position.z - ownPosition.z
        ) : null;
        visible = true;
        clearTimeout(hideTimeout);
        startHideTimeout();
    });

    startHideTimeout();
</script>

{#if visible && target != null}
    <div class="targethud" transition:fly={{ y: -10, duration: 200 }}>
        <div class="main-wrapper">
            <div class="avatar">
                <img src="{REST_BASE}/api/v1/client/resource/skin?uuid={target.uuid}" alt="avatar" />
                <div class="hurt-flash" bind:this={hurtFlash}></div>
            </div>
    
            <div class="name">{target.username}</div>
            <div class="health-stats">
                <div class="stat">
                    <div class="value">{Math.floor(target.actualHealth)}</div>
                    <img
                            class="icon"
                            src="img/hud/targethud/icon-health.svg"
                            alt="health"
                    />
                </div>
                {#if target.absorption > 0}
                    <div class="stat">
                        <div class="value">{Math.floor(target.absorption)}</div>
                        <img
                                class="icon"
                                src="img/hud/targethud/icon-absorption.svg"
                                alt="absorption"
                        />
                    </div>
                {/if}
                <div class="stat">
                    <div class="value">{Math.floor(target.armor)}</div>
                    <img
                            class="icon"
                            src="img/hud/targethud/icon-armor.svg"
                            alt="armor"
                    />
                </div>
                {#if distance !== null}
                    <div class="stat distance">
                        <div class="value">{distance.toFixed(1)}</div>
                        <span class="unit">m</span>
                    </div>
                {/if}
            </div>
            <div class="armor-stats">
                {#if target.mainHandStack.count > 0}
                    <img
                            class="held-item"
                            src={itemTextureUrl(target.mainHandStack.identifier)}
                            alt={target.mainHandStack.identifier}
                    />
                {/if}
                {#if target.armorItems[3].count > 0}
                    <ArmorStatus itemStack={target.armorItems[3]} />
                {/if}
                {#if target.armorItems[2].count > 0}
                    <ArmorStatus itemStack={target.armorItems[2]} />
                {/if}
                {#if target.armorItems[1].count > 0}
                    <ArmorStatus itemStack={target.armorItems[1]} />
                {/if}
                {#if target.armorItems[0].count > 0}
                    <ArmorStatus itemStack={target.armorItems[0]} />
                {/if}
            </div>
        </div>    
        
        <HealthProgress maxHealth={target.maxHealth + target.absorption} health={target.actualHealth + target.absorption} />
    </div>
{/if}

<style lang="scss">

    .targethud {
        background-color: var(--targethud-background-color);
        border-radius: 5px;
        overflow: hidden;
    }

    .main-wrapper {
        display: grid;
        grid-template-areas:
            "a b d"
            "a c d";
        column-gap: 10px;
        padding: 10px 15px;
    }

    .name {
        grid-area: b;
        color: var(--targethud-text-color);
        font-weight: 500;
        align-self: flex-end;
    }

    .health-stats {
        grid-area: c;
        display: flex;
        column-gap: 10px;

        .stat {
            display: flex;
            align-items: center;
            column-gap: 2px;
            white-space: nowrap;

            .value {
                color: var(--targethud-text-dimmed-color);
                font-size: 14px;
                min-width: 18px;
                display: inline-block;
            }

            &.distance .value {
                min-width: 0;
            }

            .unit {
                color: var(--targethud-text-dimmed-color);
                font-size: 12px;
            }
        }
    }

    .held-item {
        height: 30px;
        width: 30px;
        image-rendering: pixelated;
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
        background-image: url("/img/steve.png");
        background-repeat: no-repeat;
        background-size: cover;
        border-radius: 5px;
        overflow: hidden;

        // Face is the 8x8 square at (8, 8) of a 64-wide skin, at whatever resolution the texture has.
        img {
            position: absolute;
            width: 800%;
            left: -100%;
            top: -100%;
        }

        .hurt-flash {
            position: absolute;
            inset: 0;
            background-color: var(--targethud-hurt-flash-color);
            opacity: 0;
        }
    }
</style>
