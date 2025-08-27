<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {OverlayPlayListEvent, KeyEvent, ClickGuiValueChangeEvent} from "../../../integration/events";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import {onMount} from "svelte";
    import {getMinecraftKeybinds, getModuleSettings} from "../../../integration/rest";
    import type {
        ConfigurableSetting,
        MinecraftKeybind,
        MultiChooseSetting,

    } from "../../../integration/types";
    import AvatarView from "../common/PlayerView/AvatarView.svelte";
    import {scale} from "svelte/transition"
    import {REST_BASE} from "../../../integration/host";
    import type { TextComponent as TTextComponent} from "../../../integration/types";
    let keyPlayerList: MinecraftKeybind | undefined;
    let OverlayPlayList: OverlayPlayListEvent | null = null;
    let visible = false;
    let VisibilityKeywords: string[] = [];
    let columns = 1;
    let rows = 1;
    const maxHeight = 600;
    const rowHeight = 20;
    const maxColumns = 4;
    let columnWidths: number[] = [];

    function calculateLayout(players: { name: string | TTextComponent }[]) {
        const playerCount = players.length;
        const maxRows = Math.min(20, Math.floor(maxHeight / rowHeight));
        rows = Math.min(maxRows, playerCount);
        columns = Math.min(Math.ceil(playerCount / rows), maxColumns);

        while (columns > maxColumns && rows < maxRows) {
            rows++;
            columns = Math.ceil(playerCount / rows);
        }

        columnWidths = Array(columns).fill(0);
        players.forEach((player, index) => {
            const col = Math.floor(index / rows);
            const playerName = getTextString(player.name);
            const textWidth = playerName.length * 10;
            columnWidths[col] = Math.max(columnWidths[col], textWidth + 50);
        });
    }

    function getTextString(tc: string | TTextComponent): string {
        if (typeof tc === "string") return tc;
        let str = tc.text ?? "";
        if (tc.extra) {
            for (const e of tc.extra) {
                str += getTextString(e);
            }
        }
        return str;
    }

    async function handleKeyDown(event: KeyEvent) {

        if (event.key === keyPlayerList?.key.translationKey) {
            visible = event.action === 1 || event.action === 2;
            return;
        }

        if (!visible) return;
    }

    function VisibilitySetting(configurable: ConfigurableSetting) {
        const keywordsSetting = configurable.value.find(v => v.name === "Visibility") as MultiChooseSetting;
        VisibilityKeywords = keywordsSetting?.value ?? [];
    }

    async function updateKeybinds() {
        const keybinds = await getMinecraftKeybinds();
        keyPlayerList = keybinds.find(k => k.bindName === "key.playerlist");
    }

    function isVisible(visibilityType: string): boolean {
        return VisibilityKeywords.includes(visibilityType);
    }


    listen("keybindChange", updateKeybinds);
    listen("key", handleKeyDown);

    listen("overlayPlayList", (event: OverlayPlayListEvent) => {
        OverlayPlayList = event;
        if (event?.players) {
            calculateLayout(event.players);
        }
    });
    listen("betterTabValueChange", (e: ClickGuiValueChangeEvent) => {
        VisibilitySetting(e.configurable);
    });

    onMount(async () => {
        await updateKeybinds();
        const Settings = await getModuleSettings("BetterTab");
        VisibilitySetting(Settings);
    });

</script>

{#if visible}
    <div class="tab-overlay" transition:scale={{duration:300}}>
        {#if OverlayPlayList}
            <div class="tab-container hud-container">
                <!-- Header - only show if HEADER is selected in visibility settings -->
                {#if OverlayPlayList.header && isVisible("Header")}
                    <div class="tab-header">
                        <TextComponent fontSize={20} allowPreformatting={true} textComponent={OverlayPlayList.header}/>
                    </div>
                {/if}

                <!-- Player Grid - always visible when tab is open -->
                <div class="player-grid" style="grid-template-columns: {columnWidths.map(w => `minmax(360px, ${w}px)`).join(' ')};">
                    {#each OverlayPlayList.players as player, index}
                        <div class="player-entry" class:friend={player.isFriend} class:staff={player.isStaff}>
                            {#if !isVisible("NameOnly")}
                                <div class="avatar">
                                    <div class="avatar-inner">
                                        <AvatarView
                                                skinUrl={`${REST_BASE}/api/v1/client/resource/skin?uuid=${player.uuid}`}/>
                                    </div>
                                </div>
                            {/if}
                            <div class="player-name" style="max-width: {columnWidths[Math.floor(index / rows)] - 50}px;">
                                <TextComponent fontSize={20} allowPreformatting={true} textComponent={player.name}/>
                            </div>
                            <div class="player-latency">
                                <TextComponent fontSize={20} allowPreformatting={true} textComponent={player.latency}/>
                            </div>
                        </div>
                    {/each}
                </div>

                <!-- Footer - only show if FOOTER is selected in visibility settings -->
                {#if OverlayPlayList.footer && isVisible("Footer")}
                    <div class="tab-footer">
                        <TextComponent fontSize={20} allowPreformatting={true} textComponent={OverlayPlayList.footer}/>
                    </div>
                {/if}
            </div>
        {/if}
    </div>
{/if}

<style lang="scss">
  @use "../../../colors.scss" as *;

  .tab-overlay {
    position: fixed;
    width: 100%;
    height: 100%;
    top: 0;
    left: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    pointer-events: none;
  }

  .tab-container {
    position: absolute;
    top: 20px;
    left: 50%;
    transform: translateX(-50%) scale(1);
    transform-origin: center center;
    padding: 8px;
    max-width: 1500px;

  }

  .tab-header, .tab-footer {
    text-align: center;
    padding: 4px 0;
    margin-bottom: 8px;
  }

  .tab-footer {
    border-bottom: none;
    margin-top: 8px;
    margin-bottom: 0;
  }

  .player-grid {
    display: grid;
    gap: 4px;
    max-height: calc(90vh - 100px);
    overflow: hidden;
    scroll-behavior: smooth;
  }

  .avatar {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;


    .avatar-inner {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%) scale(2.5);
      transform-origin: center center;
    }
  }

  .player-entry {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 2px 4px;
    border-radius: 2px;
    min-width: 0;
  }

  .player-name {
    white-space: nowrap;
    overflow: hidden;
    flex: 1 1 auto;
    min-width: 0;
  }

  .player-latency {
    margin-left: auto;
    flex-shrink: 0;
    padding-left: 8px;
  }

  .player-entry.friend {
    background-color: rgba($blue, 0.3);
    filter: drop-shadow(0 0 4px rgba($blue, 0.6));
  }

  .player-entry.staff {
    background-color: rgba($red, 0.3);
    filter: drop-shadow(0 0 4px rgba($red, 0.6));
  }

</style>
