<script lang="ts">
    import {flip} from "svelte/animate";
    import {listen} from "../../../../integration/ws";
    import {fly} from "svelte/transition";
    import Notification from "./Notification.svelte";
    import type {NotificationEvent} from "../../../../integration/events";

    interface TNotification {
        id: number;
        title: string;
        severity: string;
        message: string;
    }

    let notifications: TNotification[] = [];

    function addNotification(title: string, message: string, severity: string) {
        const id = Date.now();
        notifications = [
            { id: id, title, message, severity },
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
    {#each notifications as { title, message, severity, id } (id)}
        <div
            animate:flip={{ duration: 200 }}
            in:fly={{ x: 30, duration: 200 }}
            out:fly={{ x: 30, duration: 200 }}
        >
            <Notification {title} {message} {severity} />
        </div>
    {/each}
</div>

<style lang="scss">
    .notifications {
        //position: fixed;
        //bottom: 5px;
        //right: 15px;
    }
</style>
