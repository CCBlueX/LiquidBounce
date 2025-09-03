<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import type {PlayerData} from "../../../integration/types";
    import {rgbaToHex, intToRgba} from "../../../integration/util";
    import {onMount} from "svelte";
    import {getPlayerData} from "../../../integration/rest";
    import {fly} from "svelte/transition";

    let playerData: PlayerData | null = null;
    let speed: number = 0;
    export let settings: { [name: string]: any };
    class SpeedDeque {
        private items: number[] = [];
        private readonly maxSize: number;
        constructor(maxSize: number = 20) { // Store last 20 speed samples (1 second at 20 TPS)
            this.maxSize = maxSize;
        }
        addBack(speed: number): void {
            this.items.push(speed);
            if (this.items.length > this.maxSize) {
                this.items.shift();
            }
        }
        getAverage(): number {
            if (this.items.length === 0) return 0;
            const sum = this.items.reduce((acc, val) => acc + val, 0);
            return sum / this.items.length;
        }
        getCurrent(): number {
            return this.items[this.items.length - 1] || 0;
        }
        clear(): void {
            this.items = [];
        }
    }
    const speedHistory = new SpeedDeque(20);

    $: if (playerData?.velocity) {
        const velocity = playerData.velocity;
        const totalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z);
        
        // Filter out micro-movements (standing still with gravity)
        const isStationary = totalSpeed < 0.08;
        
        let rawSpeed: number;
        if (isStationary) {
            rawSpeed = 0;
        } else if (settings?.unit === "b/s") {
            rawSpeed = totalSpeed * 20;
        } else {
            rawSpeed = totalSpeed * 72;
        }
        
        speedHistory.addBack(rawSpeed);
        speed = settings?.average ? speedHistory.getAverage() : speedHistory.getCurrent();
    }

    $: formattedSpeed = (() => {
        if (!playerData) return "";
        const roundedSpeed = Math.round(speed * 100) / 100;
        const unit = settings?.unit || "km/h";
        return `Speed ${roundedSpeed} ${unit}`;
    })();
    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        playerData = event.playerData;
    });
    onMount(async () => {
        const data = await getPlayerData();
        if (data) {
            playerData = data;
        }
    });
</script>

{#if playerData && formattedSpeed}
    <div 
        class="speedometer" 
        in:fly={{ y: -5, duration: 200 }}
        out:fly={{ y: -5, duration: 200 }}
        style="
            --custom-font: {settings?.font || 'Inter'};
            --custom-size: {settings?.size || 14}px;
            --custom-color: {settings?.color ? rgbaToHex(intToRgba(settings.color)) : '#ffffff'};
            {settings?.shadow?.enabled ? `
                --shadow-x: ${settings.shadow.offsetX || 0}px;
                --shadow-y: ${settings.shadow.offsetY || 0}px;
                --shadow-blur: ${settings.shadow.blurRadius || 0}px;
                --shadow-color: ${rgbaToHex(intToRgba(settings.shadow.color || -16777216))};
            ` : ''}
            {settings?.glow?.enabled ? `
                --glow-radius: ${settings.glow.radius || 5}px;
                --glow-color: ${rgbaToHex(intToRgba(settings.glow.color || -1))};
            ` : ''}
        ">
        {formattedSpeed}
    </div>
{/if}

<style lang="scss">
    @use "../../../colors.scss" as *;
    .speedometer {
        background-color: rgba($speedometer-base-color, 0.68);
        color: var(--custom-color, $speedometer-text-color);
        font-family: var(--custom-font, 'Inter');
        font-size: var(--custom-size, 14px);
        text-shadow: var(--shadow-x, 0) var(--shadow-y, 0) var(--shadow-blur, 0) var(--shadow-color, transparent);
        filter: var(--glow-enabled, none);
        border-radius: 5px;
        padding: 5px 8px;
        white-space: nowrap;
        font-weight: 500;
        user-select: none;
        pointer-events: none;
        
        &[style*="--glow-radius"] {
            filter: drop-shadow(0px 0px var(--glow-radius, 5px) var(--glow-color, #ffffff));
        }
    }
</style>
