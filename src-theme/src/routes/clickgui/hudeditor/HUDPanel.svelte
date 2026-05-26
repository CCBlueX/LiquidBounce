<script lang="ts">
    import { fade, slide } from "svelte/transition";
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

<div 
        class="panel"
        transition:fade|global={{duration: 200, easing: quintOut}}
>

    <div class="title">
        <img 
        class="icon"
        src="img/clickgui/icon-render.svg"
        alt="icon"
        />
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
                    {element.name.replace(/([a-z0-9])([A-Z])/g, '$1 $2')}
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
        display: flex;
        width: 250px; 
        max-height: 600px;
        border-radius: 5px; 
        flex-direction: column;
        overflow: hidden;
        box-shadow: 0 0 10px var(--clickgui-base-50-color); 
        backdrop-filter: blur(10px); 
        background-color: var(--clickgui-base-50-color);
    }

    .title { 
        display: grid;
        grid-template-columns: max-content 1fr max-content;
        align-items: center;
        column-gap: 12px;
        background-color: var(--clickgui-panel-header-background-color);
        border-bottom: solid 2px var(--clickgui-panel-header-border-color);
        padding: 10px 15px;

        .category {
            font-size: 14px;
            color: var(--clickgui-text-color);
            font-weight: 500;
        }
    }

    .modules { 
        padding: 5px 0; 
        overflow-y: auto;
        flex-grow: 1;
    
        &::-webkit-scrollbar { width: 0px; }
    }

    .category { 
        font-size: 13px; 
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