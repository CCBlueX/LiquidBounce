<script lang="ts">
    import { scale, slide } from "svelte/transition";
    import { quintOut } from "svelte/easing";
    import { setModuleSettings, getModuleSettings } from "../../../integration/rest";
    import { onMount } from "svelte";
    import GenericSetting from "../setting/common/GenericSetting.svelte";

    let hudConfig: any = $state(null);
    let hudElements: any[] = $state([]);

    onMount(async () => {
        try {
            hudConfig = await getModuleSettings("HUD");
            if (hudConfig) {
                const additional = hudConfig.value.find((v: any) => v.name === "AdditionalComponents")?.value || [];
                const themesCategory = hudConfig.value.find((v: any) => v.name === "Themes")?.value || [];
                const liquidBounceTheme = themesCategory.find((v: any) => v.name === "Liquidbounce")?.value || [];
                const components = liquidBounceTheme.find((v: any) => v.name === "Components")?.value || [];
                
                hudElements = [...additional, ...components].map(el => ({ ...el, expanded: false }));
            }
        } catch (e) { console.error(e); }
    });

    async function saveSettings() {
        if (!hudConfig) return;
        await setModuleSettings("HUD", hudConfig);
        hudConfig = { ...hudConfig };
    }

    async function toggleElement(element: any) {
        const settings = element.value;
        const enabledSetting = Array.isArray(settings) 
            ? settings.find((v: any) => v.name === "Enabled" || v.name === "Active")
            : null;

        if (enabledSetting) enabledSetting.value = !enabledSetting.value;
        else if (typeof element.value === 'boolean') element.value = !element.value;

        await saveSettings();
    }

    function isElementEnabled(el: any): boolean {
        if (typeof el.value === 'boolean') return el.value;
        if (Array.isArray(el.value)) {
            const enabledSetting = el.value.find((v: any) => v.name === "Enabled" || v.name === "Active");
            return enabledSetting ? !!enabledSetting.value : false;
        }
        return false;
    }
</script>

<div class="panel" transition:scale|global={{duration: 200, easing: quintOut}}>
    <div class="title">
        <span class="category">HUD Editor</span>
    </div>
    <div class="modules">
        {#each hudElements as element, i}
            <div class="module-wrapper" class:expanded={element.expanded}>
                <button 
                    class="module-item" 
                    class:enabled={isElementEnabled(element)} 
                    onclick={() => toggleElement(element)}
                    oncontextmenu={(e) => {
                        e.preventDefault();
                        element.expanded = !element.expanded;
                    }}
                >
                    {element.name}
                </button>

                {#if element.expanded && Array.isArray(element.value)}
                    <div class="settings" transition:slide={{duration: 300, easing: quintOut}}>
                        {#each element.value as setting, j}
                            <GenericSetting 
                                path="hud.{element.name}" 
                                bind:setting={element.value[j]} 
                                on:change={saveSettings}
                            />
                        {/each}
                    </div>
                {/if}
            </div>
        {/each}
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

    .title { 
        font-size: 12px; 
        color: var(--clickgui-text-color); 
        font-weight: 600; 
    }

    .panel { 
        border-radius: 15px; 
        width: 225px; 
        max-height: 600px;
        display: flex;
        flex-direction: column;
        position: absolute; 
        left: 50%; top: 50%; 
        transform: translate(-50%, -50%); 
        overflow: hidden;
        box-shadow: 0 0 10px var(--clickgui-base-50-color); 
        backdrop-filter: blur(10px); 
        background-color: var(--clickgui-base-50-color);
    }

    .title { 
        padding: 15px; 
        text-align: center;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        flex-shrink: 0;
    }

    .modules { 
        padding: 5px 0; 
        overflow-y: auto;
        flex-grow: 1;
    
        &::-webkit-scrollbar { width: 0px; }
    }

    .category { 
        font-size: 14px; 
        color: var(--clickgui-text-color); 
        font-weight: 500; 
    }

    .module-item {
        width: 100%;
        padding: 10px; 
        background: transparent; 
        border: none; 
        color: var(--clickgui-text-dimmed-color); 
        cursor: pointer; 
        text-align: center; 
    }

    .enabled { 
        color: var(--accent-color); 
    }

    .settings { 
        border-left: solid 2px var(--accent-color); 
        padding: 5px 10px; 
        margin: 0 10px 5px 10px; 
    }
</style>