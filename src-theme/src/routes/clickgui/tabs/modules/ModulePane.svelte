<script lang="ts">
    import type {ConfigurableSetting} from "../../../../integration/types";
    import type {ModuleActivationEvent} from "../../../../integration/events";
    import {getModuleSettings, setModuleEnabled, setModuleSettings} from "../../../../integration/rest";
    import {listen} from "../../../../integration/ws";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import GenericSetting from "../../setting/common/GenericSetting.svelte";
    import Switch from "../../setting/common/Switch.svelte";
    import SettingsView from "../../setting/SettingsView.svelte";
    import {byName, HEADER_SETTINGS, indexSettings, modules, refreshModule, selectModule} from "./modules.svelte";

    /**
     * The settings of one module beside the list.
     */
    let {name}: { name: string } = $props();

    let configurable = $state<ConfigurableSetting | null>(null);
    // The settings that are shown belong to this module, the name can already be the next one
    let shown = $state("");
    // Controls only read their value when they are created, so settings changed elsewhere build them again
    let reloads = $state(0);

    const module = $derived(byName(shown));
    const label = $derived($spaceSeperatedNames ? convertToSpacedString(shown) : shown);

    $effect(() => {
        load(name);
    });

    async function load(wanted: string, changedElsewhere = false) {
        const settings = await getModuleSettings(wanted);
        if (wanted !== name) {
            return;
        }
        configurable = settings;
        shown = wanted;
        if (changedElsewhere) {
            reloads++;
        }
        indexSettings(wanted, settings);
    }

    // Commands and configs change settings while the ClickGUI is closed and its page stays loaded
    listen("moduleActivation", (event: ModuleActivationEvent) => {
        if (event.moduleName === "ClickGUI") {
            load(name, true);
        }
    });
    listen("configTrackerChange", () => load(name, true));

    async function save() {
        if (!configurable) {
            return;
        }

        const saved = shown;
        await setModuleSettings(saved, $state.snapshot(configurable) as ConfigurableSetting);
        await Promise.all([load(saved), refreshModule(saved)]);
    }
</script>

{#if configurable && module}
    {#key `${shown}#${reloads}`}
        <SettingsView bind:root={configurable} bind:trail={modules.trail} highlight={modules.highlight}
                      path="clickgui.{shown}" exclude={HEADER_SETTINGS} onchange={save}>
            {#snippet header({modified, reset})}
                <header class="header">
                    <div class="title">
                        <button type="button" class="back" aria-label="Back to the list"
                                onclick={() => selectModule(null)}></button>
                        <h3>{label}</h3>
                        {#if modified}
                            <span class="changed" title="Changed from default"></span>
                            <button type="button" class="reset" title="Reset to defaults" aria-label="Reset {label}"
                                    onclick={reset}></button>
                        {/if}
                        <span class="state">
                            <Switch value={module.enabled}
                                    on:change={() => setModuleEnabled(module.name, !module.enabled)}/>
                        </span>
                    </div>
                    <p class="description">{module.description}</p>
                    <div class="quick">
                        {#each configurable!.value as setting, index (setting.name)}
                            {#if HEADER_SETTINGS.includes(setting.name)}
                                <GenericSetting path="clickgui.{shown}" bind:setting={configurable!.value[index]}
                                                on:change={save}/>
                            {/if}
                        {/each}
                    </div>
                </header>
            {/snippet}
        </SettingsView>
    {/key}
{/if}

<style lang="scss">
  .header {
    flex: none;
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 14px 18px 8px;
    border-bottom: 1px solid var(--clickgui-divider-color);
  }

  .title {
    display: flex;
    align-items: center;
    gap: 10px;
    min-height: 26px;

    h3 {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 17px;
      font-weight: 600;
    }
  }

  .changed {
    flex: none;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: var(--clickgui-changed-color);
  }

  .back, .reset {
    flex: none;
    width: 26px;
    height: 26px;
    border: none;
    border-radius: 3px;
    background: center no-repeat;
    opacity: .8;
    cursor: pointer;
    transition: ease opacity .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-control-hover-background-color);
    }
  }

  // Only where the pane has taken the place of the list
  .back {
    display: none;
    margin-left: -6px;
    background-image: url("/img/menu/icon-back.svg");
    background-size: 15px;
  }

  .reset {
    background-image: url("/img/clickgui/icon-reset.svg");
    background-size: 12px;
  }

  .state {
    flex: none;
    margin-left: auto;
  }

  .description {
    font-size: 12px;
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }

  .quick {
    display: grid;
    grid-template-columns: minmax(0, 1fr) max-content;
    column-gap: 24px;
  }

  @container workspace (max-width: 819px) {
    .back {
      display: block;
    }
  }
</style>
