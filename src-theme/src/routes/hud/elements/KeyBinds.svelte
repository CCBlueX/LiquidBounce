<script lang="ts">
    import {onMount} from "svelte";
    import {getModules, getPrintableKeyName} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import type {Module, PrintableKey} from "../../../integration/types";

    interface ModuleWithKey {
        module: Module,
        key: PrintableKey
    }

    const UNKNOWN_KEY = "key.keyboard.unknown";

    let modulesWithKey: ModuleWithKey[] = $state([]);

    async function updateModulesWithBinds() {
        const boundModules = (await getModules()).filter(m => m.keyBind.boundKey !== UNKNOWN_KEY);

        modulesWithKey = await Promise.all(
            boundModules.map(async m => ({
                module: m,
                key: await getPrintableKeyName(m.keyBind.boundKey)
            }))
        );
    }

    listen("moduleToggle", updateModulesWithBinds);
    listen("valueChanged", async (e) => {
        if (e.value.name === "Bind") {
            await updateModulesWithBinds();
        }
    })

    onMount(async () => {
        await updateModulesWithBinds();
    });
</script>

<div class="keybinds">
    <div class="header">
        <span class="title">Binds</span>
        <img class="icon" src="img/hud/keybinds/icon-keybinds.svg" alt="keybinds">
    </div>
    <div class="entries">
        {#each modulesWithKey as m (m.module.name)}
            <div class="row" class:enabled={m.module.enabled}>
                <span class="module-name">{$spaceSeperatedNames ? convertToSpacedString(m.module.name) : m.module.name}</span>
                <span class="key-bind" class:muted={!m.module.enabled}>[{m.key.localized}]</span>
            </div>
        {:else}
            <div class="no-binds">No key bindings</div>
        {/each}
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .keybinds {
    width: max-content;
    border-radius: 5px;
    overflow: hidden;
    font-size: 14px;
    min-width: 150px;
    max-width: 200px;
  }

  .header {
    background-color: rgba($keybinds-base-color, 0.68);
    padding: 7px 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      color: $keybinds-text-color;
      font-weight: 600;
    }

    .icon {
      width: 16px;
      height: 16px;
    }
  }

  .entries {
    background-color: rgba($scoreboard-base-color, 0.5);
    padding: 6px 10px;
    color: $keybinds-text-color;

    .no-binds {
      font-style: italic;
      margin-bottom: 5px;
    }
  }

  .row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 5px;
    gap: 12px;
    min-width: 0;

    &:last-child {
      margin-bottom: 0;
    }

    &.enabled {
      .module-name {
        color: $keybinds-enabled-color;
        font-weight: 500;
      }
    }

    .module-name {
      color: $keybinds-text-color;
      font-size: 14px;
      flex: 1;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .key-bind {
      font-family: monospace;
      font-size: 11px;
      color: $accent-color;
      font-weight: 600;
      flex-shrink: 0;
      min-width: max-content;

      &.muted {
        color: rgba($keybinds-text-color, 0.65);
        font-weight: 500;
      }
    }
  }
</style>
