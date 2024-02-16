<script lang="ts">
    import Status from "./Status.svelte";
    import Slot from "./Slot.svelte";
    import { listen } from "../../../../integration/ws";
    import type { PlayerStats } from "../../../../integration/types";
    import { onMount } from "svelte";
    import { getPlayerStats } from "../../../../integration/rest";

    let currentSlot = 0;
    let stats: PlayerStats | null = null;
    let maxAbsorption = 0;

    function updateStats(s: PlayerStats) {
        stats = s;
        if (stats.absorption <= 0) {
            maxAbsorption = 0;
        }
        if (stats.absorption > maxAbsorption) {
            maxAbsorption = stats.absorption;
        }
    }

    function handleMouseWheel(e: WheelEvent) {
        if (e.deltaY > 0) {
            currentSlot = (currentSlot + 1) % 9;
        } else {
            currentSlot = (currentSlot - 1 + 9) % 9;
        }
    }

    listen("playerStats", updateStats);
    onMount(async () => {
        updateStats(await getPlayerStats());
    });
</script>

<svelte:window on:wheel={handleMouseWheel} />

<div class="hotbar">
    {#if stats}
        <div class="status">
            {#if stats.armor > 0}
                <div class="pair">
                    <Status
                        max={20}
                        value={stats.armor}
                        color="#49EAD6"
                        alignRight={false}
                    />

                    <div></div>
                </div>
            {/if}
            {#if stats.absorption > 0}
                <div class="pair">
                    <Status
                        max={maxAbsorption}
                        value={stats.absorption}
                        color="#D4AF37"
                        alignRight={false}
                    />

                    <div></div>
                </div>
            {/if}
            <div class="pair">
                <Status
                    max={stats.maxHealth}
                    value={stats.health}
                    color="#FC4130"
                    alignRight={false}
                />
                <Status
                    max={20}
                    value={stats.food}
                    color="#B88458"
                    alignRight={true}
                />
            </div>
            {#if stats.experienceLevel > 0}
                <Status
                    max={100}
                    value={stats.experienceProgress * 100}
                    color="#88C657"
                    alignRight={false}
                    label={stats.experienceLevel.toString()}
                />
            {/if}
        </div>
    {/if}
    <div class="hotbar-elements">
        <div class="slider" style="left: {currentSlot * 45}px"></div>
        <div class="slots">
            <Slot />
            <Slot />
            <Slot />
            <Slot />
            <Slot />
            <Slot />
            <Slot />
            <Slot />
            <Slot />
        </div>
    </div>
</div>

<style lang="scss">
    @import "../../../../colors.scss";

    .hotbar {
        position: fixed;
        bottom: 15px;
        left: 50%;
        transform: translateX(-50%);
    }

    .pair {
        display: grid;
        grid-template-columns: 1fr 1fr;
        column-gap: 25px;
    }

    .status {
        display: flex;
        flex-direction: column;
        margin-bottom: 5px;
        row-gap: 5px;
        column-gap: 20px;
    }

    .hotbar-elements {
        background-color: rgba($hotbar-base-color, 0.68);
        position: relative;
        border-radius: 5px;
        overflow: hidden;

        .slider {
            background-color: rgba($hotbar-base-color, 0.3);
            height: 45px;
            width: 45px;
            position: absolute;
            /* transition: linear left 0.05s; TODO: Animation is possible but annoying */
        }

        .slots {
            display: flex;
        }
    }
</style>
