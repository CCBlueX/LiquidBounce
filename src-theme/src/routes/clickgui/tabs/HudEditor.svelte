<script lang="ts">
    import {onMount} from "svelte";
    import {getComponents, getMetadata, getModuleSettings, setModuleSettings} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import type {ConfigurableSetting, HudComponent, ModuleSetting, Alignment} from "../../../integration/types";
    import {HorizontalAlignment, VerticalAlignment} from "../../../integration/types";
    import type {ComponentsUpdateEvent} from "../../../integration/events";
    import DraggableComponent from "../../hud/elements/DraggableComponent.svelte";
    
    import Watermark from "../../hud/elements/Watermark.svelte";
    import ArrayList from "../../hud/elements/ArrayList.svelte";
    import TabGui from "../../hud/elements/tabgui/TabGui.svelte";
    import Notifications from "../../hud/elements/notifications/Notifications.svelte";
    import TargetHud from "../../hud/elements/targethud/TargetHud.svelte";
    import BlockCounter from "../../hud/elements/BlockCounter.svelte";
    import HotBar from "../../hud/elements/hotbar/HotBar.svelte";
    import Scoreboard from "../../hud/elements/Scoreboard.svelte";
    import GenericPlayerInventory from "../../hud/elements/inventory/GenericPlayerInventory.svelte";
    import InventoryStatistics from "../../hud/elements/inventory/InventoryStatistics.svelte";
    import Taco from "../../hud/elements/taco/Taco.svelte";
    import Keystrokes from "../../hud/elements/keystrokes/Keystrokes.svelte";
    import Effects from "../../hud/elements/Effects.svelte";
    import Text from "../../hud/elements/Text.svelte";
    import KeyBinds from "../../hud/elements/KeyBinds.svelte";
    import GenericSetting from "../setting/common/GenericSetting.svelte";

    let components: HudComponent[] = [];
    let hudSettings: ConfigurableSetting | null = null;
    let metadataId: string = "";
    let metadataName: string = "";

    let settingsAlignment: Alignment = {
        horizontalAlignment: HorizontalAlignment.LEFT,
        verticalAlignment: VerticalAlignment.TOP,
        horizontalOffset: 20,
        verticalOffset: 20
    };
    let settingsExpanded = true;

    onMount(async () => {
        const metadata = await getMetadata();
        metadataId = metadata.id;
        metadataName = metadata.name;
        components = await getComponents(metadataId);
        hudSettings = await getModuleSettings("HUD");

        const saved = localStorage.getItem("hud-editor-settings-alignment");
        if (saved) {
            settingsAlignment = JSON.parse(saved);
        }

        const savedExpanded = localStorage.getItem("hud-editor-settings-expanded");
        if (savedExpanded) {
            settingsExpanded = savedExpanded === "true";
        }
    });

    listen("componentsUpdate", (data: ComponentsUpdateEvent) => {
        if (data.id != metadataId) return;
        components = data.components;
        getModuleSettings("HUD").then(s => hudSettings = s);
    });

    async function updateHudSettings() {
        if (!hudSettings) return;
        await setModuleSettings("HUD", hudSettings);
        hudSettings = await getModuleSettings("HUD");
    }

    function updateSettingsAlignment(e: CustomEvent<Alignment>) {
        settingsAlignment = e.detail;
        localStorage.setItem("hud-editor-settings-alignment", JSON.stringify(settingsAlignment));
    }

    function toggleSettingsExpanded() {
        settingsExpanded = !settingsExpanded;
        localStorage.setItem("hud-editor-settings-expanded", settingsExpanded.toString());
    }

    async function updateComponentAlignment(componentName: string, newAlignment: any) {
        if (!hudSettings) return;
        
        const themesGroup = (hudSettings.value as ModuleSetting[]).find(s => s.name === "Themes") as ConfigurableSetting;
        if (!themesGroup) {
            console.error("Themes group not found");
            return;
        }

        const themeGroup = (themesGroup.value as ConfigurableSetting[]).find(t => 
            (t.value?.find(s => s.name === "Components") as ConfigurableSetting)?.value?.some(c => c.name === componentName));

        if (!themeGroup) {
            console.error(`Theme group for ${metadataName} or component ${componentName} not found`);
            return;
        }

        const componentsGroup = (themeGroup.value as ModuleSetting[]).find(s => s.name === "Components") as ConfigurableSetting;
        if (!componentsGroup) return;

        const componentSettings = (componentsGroup.value as ModuleSetting[]).find(s => s.name === componentName) as ConfigurableSetting;
        if (!componentSettings) return;

        const alignmentGroup = (componentSettings.value as ModuleSetting[]).find(s => s.name === "Alignment") as ConfigurableSetting;
        if (!alignmentGroup) return;

        const hAlign = (alignmentGroup.value as ModuleSetting[]).find(s => s.name === "HorizontalAlignment");
        const vAlign = (alignmentGroup.value as ModuleSetting[]).find(s => s.name === "VerticalAlignment");
        const hOffset = (alignmentGroup.value as ModuleSetting[]).find(s => s.name === "HorizontalOffset");
        const vOffset = (alignmentGroup.value as ModuleSetting[]).find(s => s.name === "VerticalOffset");

        if (hAlign) hAlign.value = newAlignment.horizontalAlignment;
        if (vAlign) vAlign.value = newAlignment.verticalAlignment;
        if (hOffset) hOffset.value = Math.round(newAlignment.horizontalOffset);
        if (vOffset) vOffset.value = Math.round(newAlignment.verticalOffset);

        await setModuleSettings("HUD", hudSettings);
        hudSettings = await getModuleSettings("HUD");
    }

</script>

<div class="hud-editor">
    {#each components as c}
        {#if c.settings.enabled}
            <DraggableComponent 
                alignment={c.settings.alignment} 
                interactive={true}
                on:change={(e) => updateComponentAlignment(c.name, e.detail)}
            >
                {#if c.name === "Watermark"}
                    <Watermark/>
                {:else if c.name === "ArrayList"}
                    <ArrayList settings={c.settings}/>
                {:else if c.name === "TabGui"}
                    <TabGui/>
                {:else if c.name === "Notifications"}
                    <Notifications/>
                {:else if c.name === "TargetHud"}
                    <TargetHud/>
                {:else if c.name === "BlockCounter"}
                    <BlockCounter settings={c.settings}/>
                {:else if c.name === "Hotbar"}
                    <HotBar/>
                {:else if c.name === "Scoreboard"}
                    <Scoreboard settings={c.settings}/>
                {:else if c.name === "ArmorItems"}
                    <GenericPlayerInventory
                            rowLength={1}
                            backgroundColor="transparent"
                            gap="2px"
                            getRenderedStacks={it => Array.from(it.armor).reverse()}
                    />
                {:else if c.name === "InventoryStatistics"}
                    <InventoryStatistics settings={c.settings} />
                {:else if c.name === "Inventory"}
                    <GenericPlayerInventory rowLength={9} getRenderedStacks={it => it.main.slice(9)} />
                {:else if c.name === "CraftingInventory"}
                    <GenericPlayerInventory rowLength={2} getRenderedStacks={it => it.crafting} />
                {:else if c.name === "EnderChestInventory"}
                    <GenericPlayerInventory rowLength={9} getRenderedStacks={it => it.enderChest} />
                {:else if c.name === "Taco"}
                    <Taco/>
                {:else if c.name === "Keystrokes"}
                    <Keystrokes/>
                {:else if c.name === "Effects"}
                    <Effects/>
                {:else if c.name === "Text"}
                    <Text settings={c.settings} />
                {:else if c.name === "Image"}
                    <img alt="" src="{c.settings.uRL}" style="scale: {c.settings.scale};">
                {:else if c.name === "KeyBinds"}
                    <KeyBinds/>
                {/if}
            </DraggableComponent>
        {/if}
    {/each}
</div>

<DraggableComponent
        alignment={settingsAlignment}
        interactive={true}
        on:change={updateSettingsAlignment}
>
    <div class="settings-panel">
        <div
                class="panel-header"
                on:contextmenu|preventDefault={toggleSettingsExpanded}
                role="button"
                tabindex="0"
        >
            <div class="panel-title-group">
                <img class="icon" src="img/hud/GG-HUD.svg" alt="icon" />
                <span class="panel-title">HUD Settings</span>
            </div>

            <button class="expand-button" on:click|stopPropagation={toggleSettingsExpanded}>
                <div class="icon" class:expanded={settingsExpanded}></div>
            </button>
        </div>

        {#if settingsExpanded && hudSettings}
            <div class="panel-content">
                <div class="settings-list">
                    {#each hudSettings.value as setting (setting.name)}
                        <GenericSetting path="clickgui.HUD" bind:setting on:change={updateHudSettings}/>
                    {/each}
                </div>
            </div>
        {/if}
    </div>
</DraggableComponent>

<style lang="scss">
    @use "../../../colors.scss" as *;

    .hud-editor {
        width: 100%;
        height: 100%;
        position: relative;
    }

    .settings-panel {
        width: 250px;
        background-color: rgba($clickgui-base-color, 0.8);
        border-radius: 5px;
        box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
        overflow: hidden;
        user-select: none;
        border: 2px solid $accent-color;
    }

    .panel-header {
        display: grid;
        grid-template-columns: 1fr max-content;
        align-items: center;
        padding: 10px 15px;
        background-color: rgba($clickgui-base-color, 0.9);
        border-bottom: solid 2px $accent-color;
        cursor: move;
    }

    .panel-title-group {
        display: flex;
        align-items: center;
        gap: 12px;

        .icon {
            width: 18px;
            height: 18px;
        }
    }

    .panel-title {
        font-size: 14px;
        color: $clickgui-text-color;
        font-weight: 500;
    }

    .panel-content {
        max-height: 545px;
        overflow-y: auto;
        overflow-x: hidden;
        background-color: rgba($clickgui-base-color, 0.8);
    }

    .panel-content::-webkit-scrollbar {
        width: 0;
    }

    .settings-list {
        display: flex;
        flex-direction: column;
    }

    .expand-button {
        background: transparent;
        border: none;
        cursor: pointer;
        padding: 0;

        .icon {
            width: 12px;
            height: 12px;
            position: relative;

            &::before, &::after {
                content: "";
                position: absolute;
                background-color: white;
                transition: transform 0.4s ease-out;
            }

            &::before {
                top: 0; left: 50%; width: 2px; height: 100%; margin-left: -1px;
            }

            &::after {
                top: 50%; left: 0; width: 100%; height: 2px; margin-top: -1px;
            }

            &.expanded {
                &::before { transform: rotate(90deg); }
                &::after { transform: rotate(180deg); }
            }
        }
    }
</style>