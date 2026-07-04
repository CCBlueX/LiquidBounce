<script lang="ts">
    import {onMount, tick} from "svelte";
    import {getComponentSettings, setComponentSettings} from "../../../../integration/rest";
    import type {Alignment, ConfigurableSetting} from "../../../../integration/types";
    import TogglableSetting from "../../setting/TogglableSetting.svelte";

    export let name: string;
    export let id: string;
    export let alignment: Alignment;

    let element: HTMLElement | undefined;
    let configurable: ConfigurableSetting | undefined;

    let bottom = false;
    let marginLeft = 0;

    const SCREEN_EDGE_MARGIN = 10;

    $: alignment.horizontalAlignment,
        alignment.horizontalOffset,
        alignment.verticalAlignment,
        alignment.verticalOffset,
        updatePosition();

    async function updatePosition() {
        await tick();

        if (!element) {
            return;
        }

        const componentBounds = element.parentElement?.getBoundingClientRect();
        if (componentBounds) {
            bottom = componentBounds.top + componentBounds.height / 2 < window.innerHeight / 2;
        }

        const bounding = element.getBoundingClientRect();

        if (bounding.right > window.innerWidth - SCREEN_EDGE_MARGIN) {
            marginLeft = window.innerWidth - SCREEN_EDGE_MARGIN - bounding.right;
        } else if (bounding.left < SCREEN_EDGE_MARGIN) {
            marginLeft = SCREEN_EDGE_MARGIN - bounding.left;
        } else {
            marginLeft = 0;
        }
    }

    async function handleSettingChange() {
        if (!configurable) {
            return;
        }

        await setComponentSettings(id, configurable);
    }

    async function loadSettings() {
        const settings = await getComponentSettings(id);
        settings.value = settings.value.filter(setting => setting.name !== "Alignment");
        configurable = settings;
    }

    onMount(() => {
        const resizeObserver = new ResizeObserver(updatePosition);

        resizeObserver.observe(element!);
        if (element?.parentElement) {
            resizeObserver.observe(element.parentElement);
        }

        window.addEventListener("resize", updatePosition);
        loadSettings();

        return () => {
            resizeObserver.disconnect();
            window.removeEventListener("resize", updatePosition);
        };
    });
</script>

<div class="settings-wrapper" class:bottom={bottom} bind:this={element}>
    <div class="settings" style="transform: translateX({marginLeft}px)">
        {#if configurable !== undefined}
            <TogglableSetting path={name} bind:setting={configurable} on:change={handleSettingChange}>
                <button
                        slot="control"
                        let:disable
                        let:label
                        class="remove-component"
                        title="Remove component"
                        on:click={disable}
                >
                    <img src="img/clickgui/icon-cross.svg" alt="">
                    <span>{label}</span>
                </button>
            </TogglableSetting>
        {/if}
    </div>
</div>

<style lang="scss">
  .settings-wrapper {
    position: absolute;
    top: -15px;
    left: 50%;
    transform: translateY(-100%) translateX(-50%);

    .settings {
      background-color: var(--clickgui-hud-editor-component-settings-background-color);
      padding: 5px 10px;
      border-radius: 5px;
      width: 200px;
      box-shadow: 0 0 10px var(--clickgui-hud-editor-component-settings-shadow-color);
      max-height: 350px;
      overflow: auto;

      &::-webkit-scrollbar {
        width: 2px;
        height: 2px;
      }

      &::-webkit-scrollbar-thumb {
        border-radius: 2px;
      }

      .remove-component {
        all: unset;
        display: flex;
        align-items: center;
        min-width: 0;
        cursor: pointer;

        img {
          display: block;
          width: 10px;
          height: 10px;
          flex: 0 0 10px;
        }

        span {
          margin-left: 7px;
          overflow: hidden;
          color: var(--clickgui-text-color);
          font-size: 12px;
          font-weight: 500;
          line-height: 12px;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    &::after {
      content: "";
      display: block;
      position: absolute;
      width: 0;
      height: 0;
      border-top: 8px solid transparent;
      border-bottom: 8px solid transparent;
      border-right: 8px solid var(--clickgui-hud-editor-component-settings-background-color);
      left: 50%;
      bottom: -12px;
      transform: translateX(-50%) rotate(-90deg);
      z-index: -1;
    }

    &.bottom {
      top: unset;
      bottom: -15px;
      transform: translateY(100%) translateX(-50%);;

      &::after {
        top: -12px;
        transform: translateX(-50%) rotate(90deg);
      }
    }

  }
</style>
