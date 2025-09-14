<script lang="ts">
    import {onMount, tick} from 'svelte';
    import type {Module} from '../../../../../integration/types';
    import {getModules} from '../../../../../integration/rest';
    import {listen} from '../../../../../integration/ws';
    import {getTextWidth} from '../../../../../integration/text_measurement';
    import {convertToSpacedString, spaceSeperatedNames} from '../../../../../theme/theme_config';
    import {flip} from 'svelte/animate';
    import {fly} from 'svelte/transition';
    import {expoOut} from "svelte/easing";

    let enabledModules: Module[] = [];

    async function updateEnabledModules() {
        const modules = await getModules();
        const visibleModules = modules.filter(m => m.enabled && !m.hidden);

        const modulesWithWidths = visibleModules.map(module => {
                let formattedName = $spaceSeperatedNames ? convertToSpacedString(module.name) : module.name;
                let fullName = module.tag == null ? formattedName : formattedName + " " + module.tag;

                return {
                    ...module,
                    width: getTextWidth(fullName, "400 16px Alibaba")
                };
            }
        );

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

    listen("moduleToggle", async () => {
        await updateEnabledModules();
    });

    listen("refreshArrayList", async () => {
        await updateEnabledModules();
    });
    listen('hudValueChange', () => updateEnabledModules());
</script>


{#each enabledModules as {name, tag} (name)}
    <div class="module" id="module" animate:flip={{duration: 350, easing: expoOut}}
         in:fly={{x: 50, duration: 250, easing: expoOut}}>
        {$spaceSeperatedNames ? convertToSpacedString(name) : name}
        {#if tag}
            <span class="tag" id="tag">&nbsp;{tag}</span>
        {/if}
    </div>
{/each}

<style lang="scss">
  @use "../../../../../colors" as *;

  ;

  .module {
    background-color: rgba($base, 0.23);
    color: $text-color;
    font-size: 16px;
    font-family: 'Alibaba', system-ui, -apple-system, sans-serif;
    padding: 5px 8px;
    width: max-content;
    font-weight: 400;
    margin-left: auto;
    text-shadow: 0 0 10px rgba(black, 0.5);
    box-shadow: -5px 0px 10px rgba($base, 0.27),
    5px 0px 10px rgba($base, 0.27);
    filter: drop-shadow(0px 0px 10px rgba($base, 1));
  }

  .tag {
    color: #AAAAAA;
  }

  .module:first-child {
    box-shadow: 0px -5px 10px rgba($base, 0.17),
    -5px 0px 10px rgba($base, 0.17),
    5px 0px 10px rgba($base, 0.17);
  }

  .module:last-child {
    box-shadow: 0 5px 10px rgba($base, 0.17),
    -5px 0px 10px rgba($base, 0.17),
    5px 0px 10px rgba($base, 0.17);
  }
</style>
