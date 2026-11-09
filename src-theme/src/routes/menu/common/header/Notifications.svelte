<script lang="ts">
    import {fly} from "svelte/transition";
    import {notification, type TNotification} from "./notification_store";
    import {onMount, tick} from "svelte";

    let current: TNotification | null = null;
    let pending: TNotification | null = null;
    let visible = false;
    let leaving = false;
    let mounted = false;
    let dismissTimeout: ReturnType<typeof setTimeout> | undefined;

    function showPending() {
        if (!mounted) {
            return;
        }

        current = pending;
        pending = null;
        leaving = false;
        visible = current !== null;
    }

    async function hideCurrent() {
        clearTimeout(dismissTimeout);
        if (leaving || !current) {
            return;
        }

        leaving = true;
        await tick();
        if (mounted) {
            visible = false;
        }
    }

    function scheduleDismiss() {
        if (!mounted || leaving || !visible || !current) {
            return;
        }

        dismissTimeout = setTimeout(hideCurrent, (current.delay ?? 3) * 1000);
    }

    onMount(() => {
        mounted = true;
        const unsubscribe = notification.subscribe((value) => {
            if (!value) {
                return;
            }

            pending = {...value};
            notification.set(null);

            if (current) {
                hideCurrent();
            } else {
                showPending();
            }
        });

        return () => {
            mounted = false;
            unsubscribe();
            clearTimeout(dismissTimeout);
        };
    });
</script>

<div class="notifications">
    {#if visible && current}
        <div class="notification"
             transition:fly|global={{duration: 500, y: -100}}
             on:introend={scheduleDismiss}
             on:outroend={showPending}>
            <div class="icon" class:error={current.error}>
                <img src="img/hud/notification/icon-info.svg" alt="info">
            </div>
            <div class="title">{current.title}</div>
            <div class="message">{current.message}</div>
        </div>
    {/if}
</div>

<style lang="scss">
  .notifications {
    position: relative;
    min-width: 350px;
  }

  .notification {
    position: relative;
    width: 100%;
    z-index: 1001;
    box-sizing: border-box;
    background-color: var(--menu-header-notification-background-color);
    border-radius: 5px;
    display: grid;
    grid-template-areas:
        "a b"
        "a c";
    grid-template-columns: max-content minmax(0, 1fr);
    overflow: hidden;
    padding-right: 10px;

    .title, .message {
      overflow-wrap: anywhere;
    }

    .title {
      color: var(--menu-text-color);
      font-weight: 500;
      font-size: 18px;
      grid-area: b;
      align-self: flex-end;
    }

    .message {
      color: var(--menu-text-dimmed-color);
      font-weight: 500;
      grid-area: c;
    }

    .icon {
      grid-area: a;
      min-height: 65px;
      width: 65px;
      background-color: var(--menu-header-notification-icon-background-color);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 10px;

      &.error {
        background-color: var(--menu-header-notification-icon-error-background-color);
      }
    }
  }
</style>
