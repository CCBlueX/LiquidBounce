<script lang="ts">
    import {fly} from "svelte/transition";
    import {notification, type TNotification} from "./notification_store";
    import {onMount} from "svelte";

    let visible = false;
    let data: TNotification | null = null;
    let hideTimeout: number | null = null;

    onMount(() => {
        visible = false;
        data = null;
        hideTimeout = null;
    });

    function show(data: TNotification | null) {
        visible = true;
        hideTimeout = setTimeout(() => {
            visible = false;
        }, data?.delay ?? (3 * 1000));
    }

    notification.subscribe((v) => {
        if (visible && hideTimeout !== null) {
            clearTimeout(hideTimeout);
            visible = false;
            setTimeout(() => {
                data = v;
                show(v);
            }, 500);
        } else {
            data = v;
            show(v);
        }
    });
</script>

<div class="notifications">
    {#if visible && data}
        <div class="notification" transition:fly|global={{duration: 500, y: -100}}>
            <div class="icon" class:error={data.error}>
                <img src="img/hud/notification/icon-info.svg" alt="info">
            </div>
            <div class="title">{data.title}</div>
            <div class="message">{data.message}</div>
        </div>
    {/if}
</div>

<style lang="scss">
  @import "../../../../colors.scss";

  .notification {
    background-color: rgba($menu-base-color, 0.68);
    border-radius: 5px;
    display: grid;
    grid-template-areas:
        "a b"
        "a c";
    grid-template-columns: max-content 1fr;
    overflow: hidden;
    padding-right: 10px;
    min-width: 350px;

    .title {
      color: $menu-text-color;
      font-weight: 600;
      font-size: 18px;
      grid-area: b;
      align-self: flex-end;
    }

    .message {
      color: $menu-text-dimmed-color;
      font-weight: 500;
      grid-area: c;
    }

    .icon {
      grid-area: a;
      height: 65px;
      width: 65px;
      background-color: $accent-color;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 10px;

      &.error {
        background-color: $menu-error-color;
      }
    }
  }
</style>