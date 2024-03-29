<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let value: boolean;
    export let name: string;

    const dispatch = createEventDispatcher();
</script>

<label class="switch">
    <input type="checkbox" bind:checked={value} on:change={() => dispatch("change")} />
    <span class="slider"></span>

    <div class="name">{name}</div>
</label>

<style lang="scss">
    @use "sass:color";
    @import "../../../../colors.scss";

    .name {
        font-weight: 500;
        color: $clickgui-text-color;
        font-size: 12px;
        margin-left: 30px;
        white-space: nowrap;
    }

    .slider {
        position: absolute;
        top: 2px;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: darken($clickgui-text-color, 55%);
        transition: ease 0.4s;
        height: 8px;
        border-radius: 4px;

        &::before {
            position: absolute;
            content: "";
            height: 12px;
            width: 12px;
            top: -2px;
            left: 0;
            background-color: $clickgui-text-color;
            transition: ease 0.4s;
            border-radius: 50%;
        }
    }

    .switch {
        position: relative;
        display: flex;
        width: 22px;
        height: 12px;
        align-items: center;
        cursor: pointer;

        input {
            display: none;
        }

        input:checked + .slider {
            background-color: color.scale(
                desaturate($accent-color, 60%),
                $lightness: -15%
            );
        }

        input:checked + .slider:before {
            transform: translateX(10px);
            background-color: $accent-color;
        }
    }
</style>
