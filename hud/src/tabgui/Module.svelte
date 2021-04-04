<script>
    import { afterUpdate } from 'svelte';

    export let name;
    export let active;
    export let enabled;

    let el;

    afterUpdate(() => {
        if (active) {
            el.scrollIntoView({block: "start", behavior: "smooth"});
        }
    });
</script>

<div bind:this={el} class="module {active ? 'active' : ''} {enabled ? 'enabled' : ''}">
    <div class="name">{name}</div>
</div>

<style>
    .module {
        font-weight: 600;
        color: #CBD1E3;
        font-size: 12px;
        padding: 6px 15px 6px 10px;
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
