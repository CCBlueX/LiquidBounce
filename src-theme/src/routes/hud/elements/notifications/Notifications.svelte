<script lang="ts">
    import {flip} from "svelte/animate";
    import {listen} from "../../../../integration/ws";
    import {fly} from "svelte/transition";
    import Notification from "./Notification.svelte";
    import type {NotificationEvent} from "../../../../integration/events";

    interface TNotification {
        id: number;
        disappearId: number;
        title: string;
        severity: string;
        message: string;
    }

    let notifications: TNotification[] = [];

    function addNotification(title: string, message: string, severity: string) {
        let id = Date.now();
        const disappearId = id;

        // Check if the notification is enabling or disabling a module
        if (severity.toString() == "ENABLED" || severity.toString() == "DISABLED") {

            // Check if there still exists an enable/disable notification for the same module
            const index = notifications.findIndex((n) => n.message === message)
            if (index !== -1) {
                // Update the id of the new notification.
                // This will make svelte able to animate it correctly
                id = notifications[index].id;

                // Remove the old notification
                notifications.splice(index, 1);
            }
        }

        notifications = [
            {id, disappearId, title, message, severity},
            ...notifications,
        ];
        setTimeout(() => {
            notifications = notifications.filter((n) => n.disappearId !== disappearId);
        }, 3000);
    }

    listen("notification", (e: NotificationEvent) => {
        addNotification(e.title, e.message, e.severity);
    });
</script>

<div class="notifications">
    {#each notifications as {title, message, severity, id} (id)}
        <div
                animate:flip={{ duration: 200 }}
                in:fly={{ x: 30, duration: 200 }}
                out:fly={{ x: 30, duration: 200 }}
        >
            <Notification {title} {message} {severity}/>
        </div>
    {/each}
</div>
