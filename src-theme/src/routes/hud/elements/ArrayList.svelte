<script lang="ts">
    import {onMount, tick} from "svelte";
    import type {Module} from "../../../integration/types";
    import {getModules} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import {getTextWidth} from "../../../integration/text_measurement";
    import {flip} from "svelte/animate";
    import {fly} from "svelte/transition";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {getPrefixAsync} from "../../../theme/arraylist";

    let enabledModules: Module[] = [];
    let prefixs = new Map();

    async function updateEnabledModules() {
        const modules = await getModules();
        const visibleModules = modules.filter(m => m.enabled && !m.hidden);

        for (let module of visibleModules) {
            if (!prefixs.has(module.name)) {
                const prefix = await getPrefixAsync(module.name);
                prefixs.set(module.name, prefix);
            }
        }

        const modulesWithWidths = visibleModules.map(module => ({
            ...module,
            width: getTextWidth($spaceSeperatedNames ? (convertToSpacedString(module.name) + prefixs.get(module.name)) : (module.name + prefixs.get(module.name)), "400 15px sf-pro")
        }));

        modulesWithWidths.sort((a, b) => b.width - a.width);

        enabledModules = modulesWithWidths;
        await tick();
    }

    spaceSeperatedNames.subscribe(async () => {
        await updateEnabledModules();
    });

    onMount(async () => {
        await updateEnabledModules();
    });

    listen("toggleModule", async () => {
        await updateEnabledModules();
    });

    listen("refreshArrayList", async () => {
        await updateEnabledModules();
    });
</script>

<div class="arraylist">
    {#each enabledModules as { name } (name)}
        <div class="module" animate:flip={{ duration: 200 }} in:fly={{ x: 50, duration: 200 }}>
            {$spaceSeperatedNames ? convertToSpacedString(name) : name} <h class="prefix">{prefixs.get(name)}</h> 
        </div>
    {/each}
</div>

<style lang="scss">
    @import "../../../colors.scss";

    .arraylist {
        //position: fixed;
        //top: 0;
        //right: 0;
    }

    .module {
        background-color: rgba($arraylist-base-color, 0.68);
        color: $arraylist-text-color;
        font-size: 14px;
        border-radius: 4px 0 0 4px;
        padding: 5px 8px;
        border-left: solid 4px $accent-color;
        width: max-content;
        font-weight: 500;
        margin-left: auto;
    }

    .prefix {
        color: $arraylist-prefix-color;
    }
</style>
