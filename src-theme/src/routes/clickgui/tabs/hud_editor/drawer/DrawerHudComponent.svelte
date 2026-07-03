<script lang="ts">

    import type {HudComponent} from "../../../../../integration/types";

    interface Props {
        component: HudComponent
    }

    const {component}: Props = $props();
</script>

<div class="drawer-hud-component">
    <div class="name">{component.name}</div>
    <div class="description">Axolotl says henlo</div>
</div>

<style lang="scss">
    .drawer-hud-component {
      background-color: var(--clickgui-module-hover-background-color);
      border: solid 2px var(--clickgui-module-hover-background-color);
      padding: 10px;
      border-radius: 5px;
      cursor: pointer;

      &:hover {
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
