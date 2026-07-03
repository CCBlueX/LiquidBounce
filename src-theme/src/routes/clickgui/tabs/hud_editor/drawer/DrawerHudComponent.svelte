<script lang="ts">

    import type {HudComponentCatalogEntry} from "../../../../../integration/types";

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
    <div class="name">{component.name}</div>
    <div class="description">{component.description}</div>
</button>

<style lang="scss">
    .drawer-hud-component {
      all: unset;
      display: block;
      box-sizing: border-box;
      width: 100%;
      background-color: var(--clickgui-module-hover-background-color);
      border: solid 2px var(--clickgui-module-hover-background-color);
      padding: 10px;
      border-radius: 5px;
      cursor: pointer;

      &:disabled {
        opacity: 0.5;
        cursor: default;
      }

      &:not(:disabled):hover {
        background-color: var(--accent-subtle-background-color);
        border: solid 2px var(--accent-color);
      }
    }

    .name {
      color: var(--text-color);
      font-weight: 600;
      margin-bottom: 5px;
      font-size: 14px;
    }

    .description {
      color: var(--clickgui-text-dimmed-color);
      font-size: 14px;
      font-weight: 500;
    }
</style>
