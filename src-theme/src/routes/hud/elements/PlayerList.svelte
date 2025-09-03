<script lang="ts">
    import { listen } from "../../../integration/ws";
    import type { OverlayPlayListEvent, KeyEvent, EventMap } from "../../../integration/events";
    import TextComponent from "../../menu/common/TextComponent.svelte";
    import { onMount } from "svelte";
    import {getTextWidth} from '../../../integration/text_measurement';
    import { getMinecraftKeybinds, getModuleSettings } from "../../../integration/rest";
    import type { ConfigurableSetting, MinecraftKeybind, MultiChooseSetting, TextComponent as TTextComponent } from "../../../integration/types";
    import AvatarView from "../common/PlayerView/AvatarView.svelte";
    import { scale } from "svelte/transition";
    import { REST_BASE } from "../../../integration/host";

    let columns = 1;
    let rows = 1;
    let visible = false;
    let columnWidths: number[] = [];
    let visibilityKeywords: string[] = [];
    let keyPlayerList: MinecraftKeybind | undefined;
    let overlayPlayList: OverlayPlayListEvent | null = null;
    const maxColumns = 4;

    const calculateLayout = (players: { name: string | TTextComponent }[]) => {
        const playerCount = players.length;
        const maxRows = Math.min(20, Math.floor(600 / 20));
        rows = Math.min(maxRows, playerCount);
        columns = Math.min(Math.ceil(playerCount / rows), maxColumns);

        while (columns > maxColumns && rows < maxRows) {
            rows++;
            columns = Math.ceil(playerCount / rows);
        }

        columnWidths = Array(columns).fill(0);
        for (const [index, player] of players.entries()) {
            const col = Math.floor(index / rows);
            const nameStr = getTextString(player.name);
            const buffer = 32 + 8 + 50 + 8;
            columnWidths[col] = Math.max(columnWidths[col], getTextWidth(nameStr,"20px Alibaba") + buffer, 360);

        }
    };

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


    const handleKeyDown = ({ key, action }: KeyEvent) => {
        if (key === keyPlayerList?.key.translationKey) {
            visible = action === 1 || action === 2;
        }
    };

    const setVisibilityKeywords = (configurable: ConfigurableSetting) => {
        const keywordsSetting = configurable.value.find(v => v.name === "Visibility") as MultiChooseSetting;
        visibilityKeywords = keywordsSetting?.value ?? [];
    };

    const updateKeybinds = async () => {
        const keybinds = await getMinecraftKeybinds();
        keyPlayerList = keybinds.find(k => k.bindName === "key.playerlist");
    };

    const isVisible = (type: string) => visibilityKeywords.includes(type);


    listen("keybindChange" as keyof EventMap, updateKeybinds);

    listen("key", handleKeyDown);

    listen("overlayPlayList", ({ players, ...rest }: OverlayPlayListEvent) => {
        overlayPlayList = { players, ...rest };
        players && calculateLayout(players);
    });

    onMount(async () => {
        await updateKeybinds();
        const settings = await getModuleSettings("BetterTab");
        setVisibilityKeywords(settings);
        setInterval(async () => {
            setVisibilityKeywords(settings);
        }, 1000);
    });
</script>


{#if visible}
    <div class="tab-overlay" transition:scale={{duration:300}}>
        {#if overlayPlayList}
            <div class="tab-container hud-container">
                <!-- Header - only show if HEADER is selected in visibility settings -->
                {#if overlayPlayList.header && isVisible("Header")}
                    <div class="tab-header">
                        <TextComponent fontSize={20} allowPreformatting={true} textComponent={overlayPlayList.header}/>
                    </div>
                {/if}

                <!-- Player Grid - always visible when tab is open -->
                <div
                        class="player-grid"
                        style="grid-template-columns: {columnWidths.map(w => `minmax(${w}px, 1fr)`).join(' ')};"
                >
                    {#each overlayPlayList.players as player}
                        <div class="player-entry"
                             class:friend={player.isFriend}
                             class:staff={player.isStaff}
                             class:self={player.isSelf}
                        >
                            {#if !isVisible("NameOnly")}
                                <div class="avatar">
                                    <div class="avatar-inner">
                                        <AvatarView
                                                skinUrl={`${REST_BASE}/api/v1/client/resource/skin?uuid=${player.uuid}`}/>
                                    </div>
                                </div>
                            {/if}
                            <div class="player-name">
                                <TextComponent fontSize={20} allowPreformatting={true} textComponent={player.name}/>
                            </div>
                            <div class="player-latency">
                                <TextComponent fontSize={20} allowPreformatting={true} textComponent={player.latency}/>
                            </div>
                        </div>
                    {/each}
                </div>

                <!-- Footer - only show if FOOTER is selected in visibility settings -->
                {#if overlayPlayList.footer && isVisible("Footer")}
                    <div class="tab-footer">
                        <TextComponent fontSize={20} allowPreformatting={true} textComponent={overlayPlayList.footer}/>
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
    width: 100%;
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

  .player-entry.self {
    background: linear-gradient(135deg in oklch,
            color-mix(in srgb, var(--secondary-color) 30%, transparent),
            color-mix(in srgb, var(--primary-color) 30%, transparent));
    filter: drop-shadow(0 0 4px color-mix(in srgb, var(--primary-color) 45%, transparent));
  }

  .player-entry.friend {
    background-color: rgba($player-entry-friend-color, 0.3);
    filter: drop-shadow(0 0 4px rgba($player-entry-friend-color, 0.45));
  }

  .player-entry.staff {
    background-color: rgba($player-entry-staff-color, 0.3);
    filter: drop-shadow(0 0 4px rgba($player-entry-staff-color, 0.45));
  }
</style>
