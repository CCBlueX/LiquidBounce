<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {REST_BASE} from "../../../../integration/host";
    import AvatarView from "../../../hud/common/PlayerView/AvatarView.svelte";
    import {removeColorCodes} from "../../../../util/color_utils";

    const dispatch = createEventDispatcher<{
        toggle: { value: string, enabled: boolean }
    }>();

    export let value: string;
    export let name: string;
    export let icon: string | undefined;
    export let enabled: boolean;

    const isPlayer = value.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);

</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="item" class:has-icon={icon !== undefined || isPlayer}
     on:click={() => dispatch("toggle", { enabled: !enabled, value })}>
    {#if isPlayer}
        <div class="avatar">
            <div class="avatar-inner">
                <AvatarView skinUrl={`${REST_BASE}/api/v1/client/resource/skin?uuid=${value}`}/>
            </div>
        </div>
    {:else if icon}
        <img class="icon" src="{icon}" alt={value}/>
    {/if}
    <div class="name">{removeColorCodes(name)}</div>
    <div class="tick">
        {#if enabled}
            <img src="img/clickgui/icon-tick-checked.svg" alt="enabled"/>
        {:else}
            <img src="img/clickgui/icon-tick.svg" alt="disabled"/>
        {/if}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .item {
    display: grid;
    grid-template-columns: 1fr max-content;
    align-items: center;
    column-gap: 5px;
    cursor: pointer;
    margin: 2px 5px 2px 0;

    &.has-icon {
      grid-template-columns: max-content 1fr max-content;
    }
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

  .icon {
    height: 25px;
    width: 25px;
  }

  .name {
    font-size: 14px;
    color: $text;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
  }
</style>
