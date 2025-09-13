<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import {popScale} from "../../../../util/animate_utils";
    import {fly} from "svelte/transition";
    import Notification from "./Notification.svelte";
    import type {NotificationEvent} from "../../../../integration/events";
    import {Howl} from "howler";

    interface TNotification {
        animationKey: number;
        id: number;
        severity: string;
        message: string;
        remaining?: number;
        intervalId?: NodeJS.Timeout;
        timeoutId?: NodeJS.Timeout;
        leaving?: boolean;
    }

    let notifications: TNotification[] = [];
    const error = new Howl({src: ['audio/notifications/error.ogg'], preload: true});
    const info = new Howl({src: ['audio/notifications/info.ogg'], preload: true});
    const success = new Howl({src: ['audio/notifications/success.ogg'], preload: true});
    const disable = new Howl({src: ['audio/notifications/disable.ogg'], preload: true, volume: 0.5});
    const enable = new Howl({src: ['audio/notifications/enable.ogg'], preload: true, volume: 0.5});
    const blink = new Howl({src: ['audio/notifications/blink.ogg'], preload: true})
    const blinked = new Howl({src: ['audio/notifications/blinked.ogg'], preload: true})

    function addNotification(title: string, message: string, severity: string) {
        const animationKey = Date.now();
        let id = animationKey;

        // Handle Blink notifications merging
        if (message.startsWith("Currently storing")) {
            const existingIndex = notifications.findIndex(n =>
                n.severity === "BLINKING" || n.severity === "BLINKED"
            );

            if (severity === "BLINK") {
                return;
            }

            if (existingIndex !== -1) {
                updateExistingNotification(existingIndex, message, severity);
                return;
            }
        }

        // Handle Enable/Disable notifications merging
        if (severity === "ENABLED" || severity === "DISABLED") {
            const existingIndex = notifications.findIndex(
                n => n.message === message && (n.severity === "ENABLED" || n.severity === "DISABLED")
            );

            if (existingIndex !== -1) {
                updateExistingNotification(existingIndex, message, severity);
                return;
            }
        }


        // Default case - create new notification
        let remaining = 3.0;
        const intervalId = setInterval(() => {
            remaining = +(remaining - 0.1).toFixed(1);
            notifications = notifications.map(n =>
                n.id === id ? {...n, remaining} : n
            );
        }, 100);

        const timeoutId = setTimeout(() => {
            clearInterval(intervalId);
            notifications = notifications.map(n =>
                n.id === id ? {...n, leaving: true} : n
            );
            setTimeout(() => {
                notifications = notifications.filter(n => n.id !== id);
            }, 10);
        }, 3000);

        notifications = [
            {
                animationKey,
                id,
                message,
                severity,
                remaining,
                intervalId,
                timeoutId
            },
            ...notifications
        ];
    }

    function updateExistingNotification(index: number, message: string, severity: string) {
        const existing = notifications[index];
        const id = existing.id;

        clearInterval(existing.intervalId);
        clearTimeout(existing.timeoutId);

        let remaining = 3.0;
        const intervalId = setInterval(() => {
            remaining = +(remaining - 0.1).toFixed(1);
            notifications = notifications.map(n =>
                n.id === id ? {...n, remaining} : n
            );
        }, 100);

        const timeoutId = setTimeout(() => {
            clearInterval(intervalId);
            notifications = notifications.map(n =>
                n.id === id ? {...n, leaving: true} : n
            );
            setTimeout(() => {
                notifications = notifications.filter(n => n.id !== id);
            }, 10);
        }, 3000);

        notifications[index] = {
            ...existing,
            message,
            severity,
            remaining: 3.0,
            intervalId,
            timeoutId,
            leaving: false
        };

        notifications = notifications;
    }

    listen("notification", (e: NotificationEvent) => {
        addNotification(e.title, e.message, e.severity);
        switch (e.severity) {
            case "ERROR":
                error.play();
                break;
            case "INFO":
                info.play();
                break;
            case "SUCCESS":
                success.play();
                break;
            case "ENABLED":
                enable.play();
                break;
            case "DISABLED":
                disable.play();
                break;
            case "BLINK":
                blink.play();
                break
            case "BLINKED":
                blinked.play();
                break
        }
    });
</script>
<div class="notifications" class:draggable={notifications.length === 0}>
    {#each notifications as notification (notification.id)}
        <div
                in:fly={{ x: 50, duration: 200 }}
                out:popScale
        >
            <Notification {...notification}/>
        </div>
    {:else}
        <!-- 空状态下的占位元素 -->
        <div class="empty-placeholder"></div>
    {/each}
</div>

<style lang="scss">
    @use "../../../../colors" as *;

  .notifications {
    will-change: transform, opacity;
    transform: translateZ(0);
    backface-visibility: hidden;
    perspective: 1000px;
    display: flex;
    bottom: 25px;
    right: 25px;
    flex-direction: column;
    align-items: flex-end;
    position: absolute;
    min-height: 80px;
    min-width: 400px;
    border-radius: 14px;
    border: 6px dashed transparent;
    transition: background-color, border-color 0.3s ease;

    &:hover {
      background: rgba(204, 204, 204, 0.2);
      border-color: #ccc;


    }

    .empty-placeholder {
      display: none;
    }


    &.draggable {
      cursor: move;

      &:hover {
        border-color: rgba(255, 255, 255, 0.8) !important;
        background: rgba(204, 204, 204, 0.3);
      }
    }
  }
</style>
