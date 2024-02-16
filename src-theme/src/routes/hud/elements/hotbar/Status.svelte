<script lang="ts">
    export let max: number;
    export let value: number;
    export let color: string;
    export let alignRight: boolean;
</script>

<div class="progress">
    <div
        class:align-right={alignRight}
        class="progress-bar"
        style="width: {Math.floor(
            (value / max) * 100,
        )}%; background-color: {color};"
    ></div>
</div>

<style lang="scss">
    .progress {
        position: relative;
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
