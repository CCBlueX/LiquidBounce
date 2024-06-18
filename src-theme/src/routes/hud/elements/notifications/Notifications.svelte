<script lang="ts">
    import {flip} from "svelte/animate";
    import {listen} from "../../../../integration/ws";
    import {fly} from "svelte/transition";
    import Notification from "./Notification.svelte";
    import type {NotificationEvent} from "../../../../integration/events";

    interface TNotification {
        animationKey: number;
        id: number;
        title: string;
        severity: string;
        message: string;
    }

    let notifications: TNotification[] = [];

    function addNotification(title: string, message: string, severity: string) {
        let animationKey = Date.now();
        const id = animationKey;

        if (severity === "ENABLED" || severity === "DISABLED") {
            // Check if there still exists an enable/disable notification for the same module
            const index = notifications.findIndex((n) => n.message === message)
            if (index !== -1) {
                // Set the id of the new notification to the old notification's id.
                // This will make svelte able to animate it correctly
                animationKey = notifications[index].animationKey;

                // Remove the old notification
                notifications.splice(index, 1);
            }
        }

        notifications = [
            {animationKey, id, title, message, severity},
            ...notifications,
        ];
        
        setTimeout(() => {
            notifications = notifications.filter((n) => n.id !== id);
        }, 3000);
    }

    listen("notification", (e: NotificationEvent) => {
        addNotification(e.title, e.message, e.severity);
    });
</script>

<div class="notifications">
    {#each notifications as {title, message, severity, animationKey} (animationKey)}
        <div
                animate:flip={{ duration: 200 }}
                in:fly={{ x: 30, duration: 200 }}
                out:fly={{ x: 30, duration: 200 }}
        >
            <Notification {title} {message} {severity}/>
        </div>
    {/each}
</div>
