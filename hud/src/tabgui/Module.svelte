<script>
    export let name;
    export let active;
    export let enabled;
</script>

<div class="module" class:active={active} class:enabled={enabled}>
    {name}
</div>

<style>
    .module {
        font-weight: 600;
        color: #CBD1E3;
        font-size: 12px;
        padding: 5px 10px;
        transition: ease padding-left .2s, ease color .2s;
    }

    .module.active {
        background-color: rgba(0, 0, 0, 0.36);
        padding-left: 15px;
    }

    .module.enabled {
        color: white;
    }
</style>
