<script lang="ts">
    import Status from "./Status.svelte";
    import Slot from "./Slot.svelte";
    import { listen } from "../../../../integration/ws";

    let currentSlot = 0;
    let maxHealth = 20;
    let health = 20;
    let hunger = 20;
    let experienceProgress = 0;

    function handleMouseWheel(e: WheelEvent) {
        if (e.deltaY > 0) {
            currentSlot = (currentSlot + 1) % 9;
        } else {
            currentSlot = (currentSlot - 1 + 9) % 9;
        }
    }

    listen("playerStats", (e: any) => {
        maxHealth = e.maxHealth;
        health = e.health;
        hunger = e.food;
        experienceProgress = e.experienceProgress * 100;
    });
</script>

<svelte:window on:wheel={handleMouseWheel} />

<div class="hotbar">
    <div class="status">
        {#if health > 20 && maxHealth > 20}
            <div class="extra-health">
                <Status
                    max={maxHealth - 20}
                    value={health - 20}
                    color="#D4AF37"
                    alignRight={false}
                />
            </div>
        {/if}
        <div class="health">
            <Status
                max={20}
                value={health > 20 ? health - (health - 20) : health}
                color="#FC4130"
                alignRight={false}
            />
        </div>
        <div class="hunger">
            <Status max={20} value={hunger} color="#B88458" alignRight={true} />
        </div>
        <div class="experience">
            <Status
                max={100}
                value={experienceProgress}
                color="#88C657"
                alignRight={false}
            />
        </div>
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

<style lang="scss">
    @import "../../../../colors.scss";

    .hotbar {
        position: fixed;
        bottom: 15px;
        left: 50%;
        transform: translateX(-50%);
    }

    .status {
        display: grid;
        grid-template-areas:
            "a b"
            "c c";
        margin-bottom: 7px;
        row-gap: 7px;
        column-gap: 20px;

        .health {
            grid-area: a;
        }

        .hunger {
            grid-area: b;
        }

        .experience {
            grid-area: c;
        }
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
