<script lang="ts">
    import type {PlayerData} from "../../../integration/types";
    import type {BlockCountChangeEvent, ClientPlayerDataEvent, ModuleToggleEvent} from "../../../integration/events";
    import {listen} from "../../../integration/ws";
    import {getModules, getPlayerData} from "../../../integration/rest";
    import {onMount,tick} from "svelte";
    import {FadeOut} from "../../../util/animate_utils";
    import {blockCount} from './island/Island';
    import {cubicOut} from "svelte/easing";
    import {Tween} from "svelte/motion";

    let playerData: PlayerData | null = null;
    let count = <number | undefined>(undefined);
    let contentElement: HTMLDivElement;
    let firstAppear = true;
    let scaffoldEnable = false
    const maxWidth = new Tween(0, { duration: 150, easing: cubicOut });

    $: if (contentElement && count !== undefined) {
        updateMaxWidth(firstAppear);
        firstAppear = false;
    } else if (count === undefined) {
        maxWidth.set(0);
        firstAppear = true;
    }

    async function updateMaxWidth(isFirstAppear = false) {
        if (isFirstAppear) {
            await maxWidth.set(0, { duration: 0 });
            await tick();
        }

        const style = getComputedStyle(contentElement);
        const fullWidth =
            contentElement.scrollWidth +
            parseFloat(style.paddingLeft) +
            parseFloat(style.paddingRight);

        await maxWidth.set(fullWidth);
    }

    listen("blockCountChange", (e: BlockCountChangeEvent) => {
        count = e.count;
        blockCount.set(e.count);
    });

    listen("clientPlayerData", (e: ClientPlayerDataEvent) => {
        playerData = e.playerData;
    });

    listen("moduleToggle", (e: ModuleToggleEvent) => {
        if (e.moduleName === "Scaffold") {
            scaffoldEnable = e.enabled;
        }
    });

    onMount(async () => {
        playerData = await getPlayerData();
        if (contentElement && count !== undefined) {
            await updateMaxWidth(true);
        }

    });
</script>

<div class="main-wrapper" class:draggable={count === undefined}>
    {#if count !== undefined && scaffoldEnable}
        <div
                class="hud hud-container"
                bind:this={contentElement}
                style="max-width: {maxWidth.current}px"
                aria-hidden={count === undefined}
                out:FadeOut|global={{ duration: 200 }}>
            {#if playerData?.mainHandStack}
                <div class="icon">
                    <svg viewBox="0 0 24 24"  xmlns="http://www.w3.org/2000/svg">
                        <path fill-rule="evenodd" clip-rule="evenodd" d="M11.7925 5.04509C11.9243 4.98497 12.0757 4.98497 12.2075 5.04509L18.2075 7.78193C18.3857 7.8632 18.5 8.04101 18.5 8.23684V15.0789C18.5 15.2585 18.4037 15.4243 18.2477 15.5133L12.2477 18.9344C12.0942 19.0219 11.9058 19.0219 11.7523 18.9344L5.75234 15.5133C5.59632 15.4243 5.5 15.2585 5.5 15.0789V8.23684C5.5 8.04101 5.61433 7.8632 5.7925 7.78193L11.7925 5.04509ZM6.5 9.04163L11.5 11.5124V17.6393L6.5 14.7885V9.04163ZM12.5 17.6393L17.5 14.7885V9.04163L12.5 11.5124V17.6393ZM12 10.644L16.8348 8.25491L12 6.04956L7.16519 8.25491L12 10.644Z" fill="#000000"/>
                    </svg>
                </div>
            {/if}
            <div class="count">Amount:
                <span class="count-number">{count}</span>
            </div>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .main-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    position: absolute;
    border: 6px dashed transparent;
    border-radius: 10px;
    min-height: 60px;
    transition: background-color, border-color 0.3s ease;
    will-change: transform;
    &:hover {
      background: rgba(204, 204, 204, 0.2);
      border-color: #ccc;
      min-width: 150px;
    }

    &.draggable {
      cursor: move;
      &:hover {
        border-color: rgba(255, 255, 255, 0.8) !important;
        background: rgba(204, 204, 204, 0.3);
      }
    }
  }

  .hud {
    display: flex;
    align-items: center;
    height: 48px;
    padding: 0 10px;
    border-radius: 12px;
    overflow: hidden;
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    white-space: nowrap;
    user-select: none;
    transition: max-width 0.15s ease;
    width: auto;
  }

  .icon {
    width: 48px;
    height: 48px;
    object-fit: contain;
    flex-shrink: 0;
    filter: brightness(0) invert(1);
  }

  .count {
    color: #CCCCCC;
    text-shadow: 0 0 3px rgba(204, 204, 204, 0.9);
    padding: 0 8px 0 0;

    .count-number {
      font-weight: bold;
    }
  }
</style>
