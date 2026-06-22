<script lang="ts">
    export let maxHealth: number;
    export let health: number;

    let width = Math.ceil(health / maxHealth * 100);
    $: width = Math.ceil(health / maxHealth * 100);
</script>

<div class="health-progress">
    <div class="thumb" style="width: {width}%;"></div>
</div>

<style lang="scss">

    .health-progress {
        position: relative;
    }

    .thumb {
        height: 8px;
        background-color: var(--targethud-health-progress-color);
        transition: ease width 0.5s;
    }
</style>