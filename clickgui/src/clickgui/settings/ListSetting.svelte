<script>
    import { sineInOut } from "svelte/easing";
    import { slide } from "svelte/transition";

    export let name;
    export let values;
    export let value;

    let expanded = false;

    function handleToggleExpand() {
        expanded = !expanded;
    }

    function handleValueChange(v) {
        value = v;
    }
</script>

<div class="setting">
    <div on:click={handleToggleExpand} class:expanded={expanded} class="name">{name} - {value}</div>
    {#if expanded}
        <div class="values" transition:slide|local={{duration: 200, easing: sineInOut}}>
            {#each values as v}
                <div class="value" on:click={handleValueChange(v)} class:enabled={v === value}>{v}</div>
            {/each}
        </div>
    {/if}
</div>

<style>
    .setting {
        padding: 7px 10px;
        overflow: hidden;
    }

    .name {
        background-color: #4677ff;
        padding: 7px 10px;
        position: relative;
        font-weight: 500;
        color: white;
        font-size: 12px;
        border-radius: 5px;
        transition: ease border-radius .2s;
    }
    
    .name.expanded {
        border-radius: 5px 5px 0px 0px;
    }

    .name::after {
        content: "";
        display: block;
        position: absolute;
        height: 10px;
        width: 10px;
        right: 10px;
        top: 50%;
        transition: ease transform .2s;
        transform: translateY(-50%);
        background-image: url("../img/settings-expand.svg");
        background-position: center;
        background-repeat: no-repeat;
    }

    .name.expanded::after {
        transform: translateY(-50%) rotate(180deg);   
    }

    .values {
        background-color: rgba(0, 0, 0, 0.5);
        border-radius: 0px 0px 5px 5px;
    }

    .values .value {
        color: rgba(255, 255, 255, 0.5);
        font-weight: 500;
        font-size: 12px;
        text-align: center;
        padding: 7px;
        transition: ease color .2s;
    }

    .values .value.enabled {
        color: #4677ff;
    }
</style>
