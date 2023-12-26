<script>
    import {sineInOut} from "svelte/easing";
    import {slide} from "svelte/transition";

    import GenericSetting from "./settings/GenericSetting.svelte";

    import { getModuleSettings, writeModuleSettings } from "../../../client/api.svelte";

    export let name;
    export let enabled;

    export let toggleModule;

    let configurable = {"value": []};

    let expanded = false;

    function handleToggle(e) {
        toggleModule(name, !enabled);

        // do not toggle here, because we want to wait for the event
        // enabled = !enabled;
    }

    function handleToggleSettings(event) {
        if (event.button === 2) {
            expanded = !expanded;

            if (expanded) {
                updateSettings();
            }
        }
    }

    function updateSettings() {
        getModuleSettings(name).then(c => {
          configurable = c;
        }).catch((e) => {
            console.error(e);
        });
    }

    function writeSettings() {
        writeModuleSettings(name, configurable).then(() => {
            updateSettings();
        }).catch((e) => {
            console.error(e);
        });
    }

    updateSettings();
</script>

<div>
    <div on:mousedown={handleToggleSettings} on:click={handleToggle} class:has-settings={configurable.value.length > 0}
         class:enabled={enabled} class:expanded={expanded} class="module" id={name + "-module"}>{name}</div>
    {#if expanded}
        <div class="settings" transition:slide={{duration: 400, easing: sineInOut}}>
            {#each configurable.value as s}
                <GenericSetting reference={s} write={writeSettings} />
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  .module {
    color: var(--textdimmed);
    text-align: center;
    font-weight: 600;
    font-size: 12px;
    padding: 10px;
    transition: ease background-color 0.2s, ease color 0.2s;
    position: relative;

    &.enabled {
      color: var(--text);
    }

    &:hover {
      background-color: rgba(0, 0, 0, 0.36);
    }

    &.has-settings {
      &::after {
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

      &.expanded::after {
        transform: translateY(-50%) rotate(0);
        opacity: 1;
      }
    }

  }

  :global(.module-highlight) {
    background-color: #4677ffa2;
  }

  .settings {
    background-color: rgba(0, 0, 0, 0.36);
    border-left: solid 4px var(--accent);
    overflow: hidden;
  }
</style>
