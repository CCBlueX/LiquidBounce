<script lang="ts">
    export let max: number;
    export let value: number;
    export let color: string;
    export let alignRight: boolean;
    export let label: string | null = null;
</script>

<div class="progress">
    {#if label}
        <div class="label">{label}</div>
    {/if}
    <div
        class:align-right={alignRight}
        class="progress-bar"
        style="width: {Math.floor(
            (value / max) * 100,
        )}%; background-color: {color};"
    ></div>
</div>

<style lang="scss">
    @import "../../../../colors.scss";

    .progress {
        position: relative;
    }

    .label {
        color: $hotbar-text-color;
        position: absolute;
        font-size: 14px;
        left: 10px;
        top: 50%;
        transform: translateY(-50%);
    }

    .progress-bar {
        border-radius: 5px;
        height: 20px;
        will-change: width;
        transition: ease width 0.2s;

        &.align-right {
            margin-left: auto;
        }
    }
</style>
