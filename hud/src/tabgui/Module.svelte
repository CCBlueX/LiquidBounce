<script>
    export let name;
    export let active;
    export let enabled;
</script>

<div class="module {active ? 'active' : ''} {enabled ? 'enabled' : ''}">
    <div class="name">{name}</div>
</div>

<style>
    .module {
        font-weight: 600;
        color: #CBD1E3;
        font-size: 12px;
        padding: 5px 15px 5px 10px;
        transition: ease color .2s;
    }

    .module.active {
        background-color: rgba(0, 0, 0, 0.36);
    }

    .name {
        transition: ease transform .2s;
    }

    .module.active .name {
        transform: translateX(5px);
    }

    .module.enabled {
        color: white;
    }
</style>
