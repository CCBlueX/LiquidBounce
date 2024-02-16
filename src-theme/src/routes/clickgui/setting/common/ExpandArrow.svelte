<script lang="ts">
    export let expanded: boolean;
</script>

<button class="arrow" class:expanded on:click={() => (expanded = !expanded)}
></button>

<style lang="scss">
    .arrow {
        width: 20px;
        position: relative;
        cursor: pointer;
        background-color: transparent;
        border: none;

        &::after {
            content: "";
            display: block;
            position: absolute;
            height: 10px;
            width: 10px;
            right: 0;
            top: 50%;
            background-image: url("img/clickgui/icon-settings-expand.svg");
            background-position: center;
            background-repeat: no-repeat;
            transform-origin: 50% 50%;
            transform: translateY(-50%) rotate(-90deg);
            transition:
                ease opacity 0.2s,
                ease transform 0.4s;
        }

        &.expanded::after {
            transform: translateY(-50%) rotate(0);
            opacity: 1;
        }
    }
</style>
