<script lang="ts">
    import {fly} from "svelte/transition";
    import {notification, type TNotification} from "./notification_store";
    import {onMount} from "svelte";
    import {get} from "svelte/store";
    import {cubicOut} from 'svelte/easing';
    import {Tween} from "svelte/motion";

    let progress = new Tween(1, {duration: 0, easing: cubicOut});

    let currentNotification: TNotification | null = null;
    let showNotification = false;
    let timeoutHandle: ReturnType<typeof setTimeout> | null = null;
    progress.set(1, {duration: 0});


    onMount(() => {
        if (get(notification)) notification.set(null);

        const unsubscribe = notification.subscribe((n) => {
            if (n) {

                if (timeoutHandle) {
                    clearTimeout(timeoutHandle);
                    timeoutHandle = null;
                }

                currentNotification = n;
                showNotification = true;
                progress.set(1, {duration: 0});
                timeoutHandle = setTimeout(() => {
                    showNotification = false;
                    timeoutHandle = null;
                }, (n.delay ?? 3) * 1000);

                progress.set(0, {
                    duration: (n.delay ?? 3) * 1000,
                    easing: cubicOut
                });
            } else {
                showNotification = false;
            }
        });

        return () => {
            unsubscribe();
            if (timeoutHandle) clearTimeout(timeoutHandle);
        };
    });

</script>


<div class="notifications">
    {#if showNotification && currentNotification}
        {#key currentNotification.id || currentNotification.message}
            <!-- svelte-ignore a11y-click-events-have-key-events -->
            <!-- svelte-ignore a11y-no-static-element-interactions -->
            <div class="notification"
                 transition:fly|global={{duration: 500, y: -100}}
                 style="--progress: {progress.current}"
                 on:click={() => {
    if (currentNotification?.url) {
        navigator.clipboard.writeText(currentNotification.url)
            .then(() => {
                               if (currentNotification) {
                    currentNotification.message = "Link copied!";
                }
            })
            .catch(err => console.error("Copy failed", err));
    }
}}
                 on:outroend={() => {
        if (!showNotification) currentNotification = null;
     }}>

                <div class="icon" class:error={currentNotification.error}>
                    <img src="img/hud/notification/icon-info.svg" alt="info">
                </div>
                <div class="title">{currentNotification.title}</div>
                <div class="message">{currentNotification.message}</div>
            </div>
        {/key}
    {/if}
</div>
<style lang="scss">
  @use "../../../../colors.scss" as *;

  .notifications {
    display: grid;
    grid-template-columns: 1fr;
  }

  .notification {
    position: relative;
    grid-row-start: 1;
    grid-column-start: 1;
    background: rgba(255, 255, 255, 0.05);
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
    border-radius: 5px;
    display: grid;
    grid-template-areas:
        "a b"
        "a c";
    grid-template-columns: max-content 1fr;
    overflow: hidden;
    padding-right: 10px;
    min-width: 350px;

    &::after {
      content: "";
      position: absolute;
      top: 0;
      right: 0;
      height: 100%;
      width: calc(100% * var(--progress));
      background: linear-gradient(to left, rgba(100, 150, 255, 0.2), rgba(100, 150, 255, 0));
      pointer-events: none;
      transition: width 0.1s linear;
      z-index: 0;
    }

    > * {
      position: relative;
      z-index: 1;
    }

    .title {
      color: $text;
      font-weight: 600;
      font-size: 18px;
      grid-area: b;
      align-self: flex-end;
    }

    .message {
      color: $text-color;
      font-weight: 500;
      grid-area: c;
    }

    .icon {
      grid-area: a;
      height: 65px;
      width: 65px;
      background-color: rgba(255, 255, 255, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 10px;
    }
  }
</style>
