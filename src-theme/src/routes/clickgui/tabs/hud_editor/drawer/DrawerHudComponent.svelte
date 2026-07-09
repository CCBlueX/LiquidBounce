<script lang="ts">

    import type {HudComponentCatalogEntry} from "../../../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../../theme/theme_config";

    interface Props {
        component: HudComponentCatalogEntry,
        onselect: (component: HudComponentCatalogEntry) => void
    }

    const {component, onselect}: Props = $props();
</script>

<button
        type="button"
        class="drawer-hud-component"
        disabled={!component.canAdd}
        onclick={() => onselect(component)}
>
    <span class="name">{$spaceSeperatedNames ? convertToSpacedString(component.name) : component.name}</span>
    <span class="description">{component.description}</span>
</button>

<style lang="scss">
  .drawer-hud-component {
    all: unset;
    display: block;
    box-sizing: border-box;
    width: 100%;
    background-color: var(--clickgui-hud-editor-drawer-component-background-color);
    border: solid 2px var(--clickgui-hud-editor-drawer-component-border-color);
    padding: 10px;
    border-radius: 5px;
    cursor: pointer;

    &:disabled {
      opacity: 0.5;
      cursor: default;
    }

    &:not(:disabled):hover {
      background-color: var(--clickgui-hud-editor-drawer-component-hover-background-color);
      border-color: var(--clickgui-hud-editor-drawer-component-hover-border-color);
    }
  }

  .name {
    color: var(--clickgui-hud-editor-drawer-component-name-color);
    font-weight: 600;
    margin-bottom: 5px;
    font-size: 14px;
    display: block;
  }

  .description {
    color: var(--clickgui-hud-editor-drawer-component-description-color);
    font-size: 14px;
    font-weight: 500;
    display: block;
  }
</style>
