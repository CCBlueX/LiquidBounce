<script lang="ts">
    export let maxHealth: number;
    export let health: number;

    let width = Math.ceil(health / maxHealth * 100);
    $: width = Math.ceil(health / maxHealth * 100);
</script>

<div class="health-progress">
    <div class="trail" style="width: {width}%;"></div>
    <div class="thumb" style="width: {width}%;"></div>
</div>

<style lang="scss">

    .health-progress {
        position: relative;
        height: 8px;
    }

    .trail, .thumb {
        position: absolute;
        top: 0;
        left: 0;
        height: 100%;
    }

    .trail {
        background-color: var(--targethud-health-trail-color);
        transition: ease width 0.6s 0.4s;
    }

    .thumb {
        background-color: var(--targethud-health-progress-color);
        transition: ease width 0.2s;
    }
</style>
