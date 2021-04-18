<script>
    import { sineInOut } from "svelte/easing";
    import { slide } from "svelte/transition";

    import BooleanSetting from "./settings/BooleanSetting.svelte";
    import RangeSetting from "./settings/RangeSetting.svelte";
    import ListSetting from "./settings/ListSetting.svelte";
    import ColorSetting from "./settings/ColorSetting.svelte";
    import TextSetting from "./settings/TextSetting.svelte";

    export let name;
    export let enabled;
    export let settings;
    export let setEnabled;

    let expanded = false;

    function handleToggle(e) {
        setEnabled(!enabled);
    }

    function handleToggleSettings(event) {
        if (event.button === 2) {
            expanded = !expanded;
        }  
    }
</script>

<div>
    <div on:mousedown={handleToggleSettings} on:click={handleToggle} class:has-settings={settings.length > 0} class:enabled={enabled} class:expanded={expanded} class="module">{name}</div>
    {#if expanded}
        <div class="settings" transition:slide|global={{duration: 400, easing: sineInOut}}>
            {#each settings as s}
                {#if s.type === "boolean"}
                    <BooleanSetting name={s.name} value={s.value} setValue={s.setValue}></BooleanSetting>
                {:else if s.type === "range"}
                    <RangeSetting name={s.name} min={s.min} max={s.max} step={s.step} value1={s.value1} value2={s.value2} setValue1={s.setValue1} setValue2={s.setValue2}></RangeSetting>
                {:else if s.type === "list"}
                    <ListSetting name={s.name} values={s.values} value={s.value}></ListSetting>
                {:else if s.type === "color"}
                    <ColorSetting name={s.name} value={s.value.toUpperCase()}></ColorSetting>
                {:else if s.type === "text"}
                    <TextSetting name={s.name} value={s.value}></TextSetting>
                {/if}
            {/each}
        </div>
    {/if}
</div>

<style>
    .module {
        color: #CBD1E3;
        text-align: center;
        font-weight: 600;
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
        transition: ease opacity 0.2s, ease transform 0.4s;
    }

    .settings {
        background-color: rgba(0, 0, 0, 0.36);
        border-left: solid 4px #4677FF;
        overflow: hidden;
    }

    .module.has-settings.expanded::after {
        transform: translateY(-50%) rotate(0);
        opacity: 1;
    }
</style>
