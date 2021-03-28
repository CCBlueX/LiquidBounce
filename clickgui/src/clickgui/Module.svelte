<script>
    export let name;
    export let enabled;
    export let hasSettings;
</script>

<div class="module">{name}</div>

<style>
    .module {
        color: rgba(255, 255, 255, 0.5);
        text-align: center;
        font-weight: 500;
        font-size: 12px;
        padding: 10px;
        transition: ease background-color 0.2s, ease color 0.2s;
        position: relative;
    }

    .module.enabled {
        color: white;
    }

    .module:hover {
        background-color: rgba(0, 0, 0, 0.36);
    }

    .module.has-settings::after {
        content: "";
        display: block;
        position: absolute;
        height: 10px;
        width: 10px;
        right: 15px;
        top: 50%;
        background-image: url("../img/settings-expand.svg");
        background-position: center;
        background-repeat: no-repeat;
        opacity: 0.5;
        transform-origin: 50% 50%;
        transform: translateY(-50%) rotate(-90deg);
        transition: ease opacity 0.2s, ease transform 0.2s;
    }

    .module.has-settings.expanded::after {
        transform: translateY(-50%) rotate(0);
        opacity: 1;
    }
</style>
