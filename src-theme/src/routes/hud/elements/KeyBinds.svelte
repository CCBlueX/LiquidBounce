<script lang="ts">
    import {onMount} from "svelte";
    import {getModules} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import type {Module} from "../../../integration/types";
    import {UNKNOWN_KEY} from "../../../util/utils";
    import BindDisplay from "../../clickgui/setting/bind/BindDisplay.svelte";

    let modules: Module[] = $state([]);

    async function updateModulesWithBinds() {
        modules = (await getModules()).filter(m => m.keyBind.boundKey !== UNKNOWN_KEY);
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
        {#each modules as m (m.name)}
            <div class="row" class:enabled={m.enabled}>
                <span class="module-name">{$spaceSeperatedNames ? convertToSpacedString(m.name) : m.name}</span>
                <span class="key-bind" class:muted={!m.enabled}>
                    [<BindDisplay boundKey={m.keyBind.boundKey} modifiers={m.keyBind.modifiers}/>]
                </span>
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
      display: inline-flex;
      align-items: center;
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
