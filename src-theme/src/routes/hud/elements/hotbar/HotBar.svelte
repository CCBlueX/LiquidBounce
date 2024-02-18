<script lang="ts">
    import Status from "./Status.svelte";
    import Slot from "./Slot.svelte";
    import {listen} from "../../../../integration/ws";
    import type {PlayerData} from "../../../../integration/types";
    import {onMount} from "svelte";
    import {getPlayerData} from "../../../../integration/rest";

    let currentSlot = 0;
    let playerData: PlayerData | null = null;
    let maxAbsorption = 0;

    function updatePlayerData(s: PlayerData) {
        playerData = s;
        if (playerData.absorption <= 0) {
            maxAbsorption = 0;
        }
        if (playerData.absorption > maxAbsorption) {
            maxAbsorption = playerData.absorption;
        }
        currentSlot = playerData.selectedSlot;
    }

    listen("clientPlayerData", (event: any) => {
        updatePlayerData(event.playerData);
    });
    onMount(async () => {
        updatePlayerData(await getPlayerData());
    });
</script>

{#if playerData && playerData.gameMode !== "spectator"}
    <div class="hotbar">
        <div class="status">
            {#if playerData.armor > 0}
                <div class="pair">
                    <Status
                            max={20}
                            value={playerData.armor}
                            color="#49EAD6"
                            alignRight={false}
                    />

                    <div></div>
                </div>
            {/if}
            {#if playerData.gameMode !== "creative"}
                {#if playerData.absorption > 0}
                    <div class="pair">
                        <Status
                                max={maxAbsorption}
                                value={playerData.absorption}
                                color="#D4AF37"
                                alignRight={false}
                        />

                        <div></div>
                    </div>
                {/if}
                <div class="pair">
                    <Status
                            max={playerData.maxHealth}
                            value={playerData.health}
                            color="#FC4130"
                            alignRight={false}
                    />
                    <Status
                            max={20}
                            value={playerData.food}
                            color="#B88458"
                            alignRight={true}
                    />
                </div>
            {/if}
            {#if playerData.experienceLevel > 0}
                <Status
                        max={100} value={playerData.experienceProgress * 100}
                        color="#88C657"
                        alignRight={false}
                        label={playerData.experienceLevel.toString()}
                />
            {/if}

        </div>

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
{/if}

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
