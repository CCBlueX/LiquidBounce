<script lang="ts">
    import { onMount, onDestroy } from "svelte";
    import { fly } from "svelte/transition";
    import { flip } from "svelte/animate";
    import type { Unsubscriber } from "svelte/store";
    import { getModules } from "../../../integration/rest";
    import { listen } from "../../../integration/ws";
    import { convertToSpacedString, spaceSeperatedNames } from "../../../theme/theme_config";
    import { hasValidKeyBind, createModuleWithKeyBind, type ModuleWithKeyBind } from "../../../integration/key_utils";

    let modulesWithBinds: ModuleWithKeyBind[] = [];

    const cleanupFunctions: Array<() => void> = [];

    function getDisplayName(moduleName: string): string {
        return $spaceSeperatedNames ? convertToSpacedString(moduleName) : moduleName;
    }

    async function updateModulesWithBinds(): Promise<void> {
        try {
            const modules = await getModules();
            const filtered = modules.filter(hasValidKeyBind);

            modulesWithBinds = await Promise.all(
                filtered.map(module => createModuleWithKeyBind(module, getDisplayName(module.name)))
            );
        } catch {
            modulesWithBinds = [];
        }
    }

    onMount(() => {
        const unsubscribe: Unsubscriber = spaceSeperatedNames.subscribe(updateModulesWithBinds);
        cleanupFunctions.push(unsubscribe);

        cleanupFunctions.push(listen("moduleToggle", updateModulesWithBinds));
        cleanupFunctions.push(listen("clickGuiValueChange", updateModulesWithBinds));

        updateModulesWithBinds();
    });

    onDestroy(() => {
        cleanupFunctions.forEach(cleanup => cleanup());
    });
</script>

<div class="keybinds-panel" transition:fly={{ y: -10, duration: 200 }}>
    <div class="header">
        <span class="title">Binds</span>
        <img class="icon" src="img/hud/keybinds/icon-keybinds.svg" alt="keybinds" />
    </div>
    <div class="entries">
        {#each modulesWithBinds as { module, keyName, displayName } (module.name)}
            <div 
                class="row" 
                class:enabled={module.enabled}
                animate:flip={{ duration: 200 }}
            >
                <span class="module-name">{displayName}</span>
                <span class="key-bind" class:muted={!module.enabled}>[{keyName}]</span>
            </div>
        {:else}
            <div class="no-binds">No key bindings</div>
        {/each}
    </div>
</div>

<style lang="scss">
    @use "../../../colors.scss" as *;

    .keybinds-panel {
        width: max-content;
        border-radius: 5px;
        overflow: hidden;
        font-size: 14px;
        min-width: 150px;
        max-width: 200px;
    }

    .header {
        background-color: rgba($keybinds-base-color, 0.78);
        padding: 7px 10px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .title {
            color: $keybinds-text-color;
            font-weight: 600;
            font-size: 14px;
        }
        
        .icon {
            width: 14px;
            height: 14px;
            color: $keybinds-text-color;
            opacity: 0.8;
        }
    }

    .entries {
        background-color: rgba($keybinds-base-color, 0.58);
        padding: 8px 10px;
        
        .no-binds {
            color: $keybinds-text-dimmed-color;
            font-size: 14px;
            font-weight: 500;
            text-align: center;
            font-style: italic;
        }
    }

    .row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 4px;
        color: $keybinds-text-dimmed-color;
        gap: 8px;
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
            color: $keybinds-text-dimmed-color;
            font-size: 14px;
            flex: 1;
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .key-bind {
            font-family: monospace;
            font-size: 12px;
            color: $accent-color;
            font-weight: 600;
            flex-shrink: 0;
            min-width: max-content;
            
            &.muted {
                color: rgba($keybinds-text-dimmed-color, 0.7);
                font-weight: 400;
            }
        }
    }
</style>