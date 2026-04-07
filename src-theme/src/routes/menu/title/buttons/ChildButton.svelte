<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import ToolTip from "../../common/ToolTip.svelte";
    import TitleButtonIcon from "./TitleButtonIcon.svelte";

    export let title: string;
    export let icon: string;
    export let parentHovered: boolean;

    const dispatch = createEventDispatcher();
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="child-button" on:click|stopPropagation={() => dispatch("click")} class:parent-hovered={parentHovered}>
    <ToolTip color="var(--menu-base-color)" text="Join Realms server" />

    <div class="icon">
        <TitleButtonIcon {icon} />
    </div>

    <div class="title">{title}</div>
</div>

<style lang="scss">

    .child-button {
      position: relative;
      display: flex;
      align-items: center;
      border-radius: 5px;
      background-color: var(--menu-child-button-background-color);
      transition: ease background-color .2s;
      padding: 15px;

      &.parent-hovered {
        background-color: var(--menu-child-button-hover-background-color);

        .icon {
          color: var(--menu-child-button-icon-hover-color);
        }

        .title {
          color: var(--menu-child-button-hover-text-color);
        }
      }
    }

    .title {
      color: var(--menu-text-color);
      font-weight: 600;
      font-size: 16px;
      transition: ease color 0.2s;
      margin-left: 10px;
    }

    .icon { /* necessary because svelte's transition system sucks */
      color: var(--menu-child-button-icon-color);
      width: 28px;
      height: 28px;
      transition: ease color 0.2s;
    }
</style>
