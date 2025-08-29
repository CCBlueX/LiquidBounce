<script lang="ts">
    import {onMount, onDestroy} from 'svelte';
    import type {Module} from '../../../../integration/types';
    import {getModules} from '../../../../integration/rest';
    import {listen} from '../../../../integration/ws';
    import {getTextWidth} from '../../../../integration/text_measurement';
    import {convertToSpacedString, spaceSeperatedNames} from '../../../../theme/theme_config';
    import {getPrefixAsync} from './arraylist';
    import {flip} from 'svelte/animate';
    import {fly} from 'svelte/transition';

    import {
        subscribeColors,
        arraylistGradient,
        destroyGradient,
        subscribeRenderSettings
    } from './arraylist';

    let enabledModules: Array<Module & { prefix: string; formattedName: string; width: number }> = [];
    let intervalId: number;
    let unsubs: [() => void, () => void];
    let unsubRenderSettings: () => void;

    async function updateEnabledModules() {
        await document.fonts.load("500 16px 'Product Sans'");

        const modules = await getModules();
        const visible = modules.filter((m) => m.enabled && !m.hidden);

        const prefixMap = new Map<string, string>();
        await Promise.all(
            visible.map(async (m) => {
                const prefix = await getPrefixAsync(m.name);
                prefixMap.set(m.name, prefix);
            })
        );

        const measuredModules = await Promise.all(visible.map(async (m) => {
            const formattedName = $spaceSeperatedNames
                ? convertToSpacedString(m.name)
                : m.name;
            const prefix = prefixMap.get(m.name) || '';
            const showPrefix = prefix.trim().length > 0;
            const fullName = showPrefix ? `${formattedName}\u00A0${prefix}` : formattedName;

            const width = getTextWidth(
                fullName,
                "normal 500 16px 'Product Sans', system-ui, sans-serif"
            );

            return {
                ...m,
                formattedName,
                prefix,
                width
            };
        }));

        measuredModules.sort((a, b) => b.width - a.width);

        enabledModules = measuredModules;
    }

    onMount(async () => {

        await updateEnabledModules();

        listen('moduleToggle', () => updateEnabledModules());
        listen('refreshArrayList', () => updateEnabledModules());
        listen('hudValueChange', () => updateEnabledModules());
        spaceSeperatedNames.subscribe(() => updateEnabledModules());

        unsubs = subscribeColors();
        unsubRenderSettings = subscribeRenderSettings(() => {
            updateEnabledModules();
        });
        setTimeout(() => arraylistGradient(), 50);

        intervalId = window.setInterval(arraylistGradient, 50);
    });

    onDestroy(() => {
        unsubRenderSettings?.();
        destroyGradient(intervalId, unsubs);
    });
</script>


{#each enabledModules as {formattedName, prefix, name} (name)}
    <div
            class="module"
            id="module-name"
            animate:flip={{ duration: 200 }}
            in:fly={{ x: 50, duration: 200 }}
    >
        {formattedName}&nbsp;
        {#if prefix}<span class="prefix">{prefix}</span>{/if}
        <span class="side-bar" id="side-bar"></span>
    </div>
{/each}


<style lang="scss">
    @use "../../../../colors" as *;;


  .module {
    position: relative;
    display: flex;
    align-items: center;
    background-color: rgba(0, 0, 0, 0.1);
    box-shadow: -10px 0px 20px rgba(0, 0, 0, 0.15),
    10px 0px 20px rgba(0, 0, 0, 0.15);
    color: white;
    text-shadow: 0 0 0.5px white;
    font-family: 'Product Sans', system-ui, -apple-system, sans-serif;
    font-size: 16px;
    padding: 2px 6px;
    width: max-content;
    font-weight: 500;
    text-transform: capitalize;
    border-radius: 3px;

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: inherit;
      z-index: -1;
    }

    &:first-child {
      border-radius: 3px 3px 0 0;

      &::after {
        border-radius: 3px 3px 0 0;
      }
    }

    &:last-child {
      border-radius: 0 0 3px 3px;

      &::after {
        border-radius: 0 0 3px 3px;
      }
    }
  }

  .prefix {
    color: #AAAAAA;
    text-shadow: 0 0 3px rgba(#AAAAAA, 0.9);
    font-family: 'Product Sans', system-ui, -apple-system, sans-serif;
  }

  .side-bar {
    position: absolute;
    right: -3px;
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: calc(100% - 6px);
    background-color: currentColor;
    border-radius: 2px;
    box-shadow: 0 0 5px currentColor;
    pointer-events: none;
  }
</style>
