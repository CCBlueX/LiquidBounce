<script lang="ts">
    import {onMount} from "svelte";
    import type {BooleanSetting, ConfigurableSetting, ModuleSetting} from "../../../integration/types";
    import {getGlobalSettings, setGlobalSettings} from "../../../integration/rest";
    import {persistentDataLoaded, setItem} from "../../../integration/persistent_storage";
    import {convertToSpacedString} from "../../../theme/theme_config";
    import SideNav, {type NavItem} from "../common/SideNav.svelte";
    import Switch from "../setting/common/Switch.svelte";
    import SettingsView from "../setting/SettingsView.svelte";
    import {isModified} from "../setting/defaults";

    const GROUP_KEY = "clickgui.settings.group";

    let globalSettings = $state<ConfigurableSetting | null>(null);
    let active = $state("");
    let trail = $state<string[]>([]);

    const groups = $derived((globalSettings?.value ?? [])
        .filter(group => group.valueType === "CONFIGURABLE" || group.valueType === "TOGGLEABLE"));

    const items = $derived<NavItem[]>(groups.map(group => ({
        id: group.name,
        label: convertToSpacedString(group.name),
        changed: isModified(group),
    })));

    const group = $derived(groups.find(candidate => candidate.name === active));
    const enabled = $derived(group?.valueType === "TOGGLEABLE"
        ? (group.value as ModuleSetting[])[0] as BooleanSetting
        : null);

    async function load() {
        globalSettings = await getGlobalSettings();
    }

    async function save() {
        if (!globalSettings) {
            return;
        }

        await setGlobalSettings($state.snapshot(globalSettings) as ConfigurableSetting);
        await load();
    }

    function select(name: string) {
        active = name;
        trail = [];
        setItem(GROUP_KEY, name);
    }

    onMount(async () => {
        await Promise.all([load(), persistentDataLoaded]);
        const stored = localStorage.getItem(GROUP_KEY);
        active = groups.find(candidate => candidate.name === stored)?.name ?? groups[0]?.name ?? "";
    });
</script>

<div class="settings">
    <SideNav {items} {active} label="Settings" onselect={select}/>

    <section class="main">
        {#if globalSettings && group}
            {#key active}
                <div class="column">
                    <SettingsView bind:root={globalSettings} home={[active]} bind:trail path="clickgui.global"
                                  onchange={save}>
                        {#snippet header({modified, reset})}
                            <header class="header">
                                <h3>{convertToSpacedString(active)}</h3>
                                {#if modified}
                                    <span class="changed" title="Changed from default"></span>
                                    <button type="button" class="reset" title="Reset to defaults"
                                            aria-label="Reset {convertToSpacedString(active)}" onclick={reset}></button>
                                {/if}
                                {#if enabled}
                                    <span class="state">
                                        <Switch bind:value={enabled.value} on:change={save}/>
                                    </span>
                                {/if}
                            </header>
                        {/snippet}
                    </SettingsView>
                </div>
            {/key}
        {/if}
    </section>
</div>

<style lang="scss">
  .settings {
    flex: 1;
    min-width: 0;
    display: flex;
  }

  .main {
    flex: 1;
    min-width: 0;
    display: flex;
    justify-content: center;
    background-color: var(--clickgui-pane-background-color);
  }

  .column {
    flex: 0 1 640px;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  .header {
    flex: none;
    display: flex;
    align-items: center;
    gap: 10px;
    min-height: 62px;
    padding: 0 18px;
    border-bottom: 1px solid var(--clickgui-divider-color);

    h3 {
      font-size: 18px;
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

  .reset {
    flex: none;
    width: 26px;
    height: 26px;
    border: none;
    border-radius: 3px;
    background: url("/img/clickgui/icon-reset.svg") center / 12px no-repeat;
    opacity: .8;
    cursor: pointer;
    transition: ease opacity .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-control-hover-background-color);
    }
  }

  .state {
    flex: none;
    margin-left: auto;
  }
</style>
