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

        const intervalId = setInterval(updateModulesWithBinds, 2000);
        cleanupFunctions.push(() => clearInterval(intervalId));

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
        background-color: rgba($keybinds-base-color, 0.8);
        padding: 8px 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .title {
            color: $keybinds-text-color;
            font-weight: 600;
            font-size: 14px;
        }
        
        .icon {
            width: 16px;
            height: 16px;
            filter: brightness(0) invert(1);
            opacity: 0.95;
        }
    }

    .entries {
        background-color: rgba($keybinds-base-color, 0.6);
        padding: 6px 12px;
        
        .no-binds {
            color: $keybinds-text-dimmed-color;
            font-size: 13px;
            font-weight: 400;
            text-align: center;
            font-style: italic;
            padding: 2px 0;
        }
    }

    .row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 5px;
        color: $keybinds-text-dimmed-color;
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
            color: $keybinds-text-dimmed-color;
            font-size: 13px;
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
                color: rgba($keybinds-text-dimmed-color, 0.65);
                font-weight: 500;
            }
        }
    }
</style>