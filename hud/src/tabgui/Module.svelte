<script>
    export let name;
    export let active;
    export let enabled;
</script>

<div class="module {active ? 'active' : ''} {enabled ? 'enabled' : ''}">
    <div class="unskew">{name}</div>
</div>

<style>
    .module {
        font-weight: 500;
        color: #CBD1E3;
        font-size: 12px;
        padding: 5px 10px;
    }

    .unskew {
        transform: skew(5deg, 0);
    }

    .module.active {
        color: white;
        background-color: rgba(0, 0, 0, 0.36);
    }

    .module.enabled {
        color: #4677ff;
    }
</style>
