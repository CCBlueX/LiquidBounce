<script>
    import { flip } from "svelte/animate";
    import { fly } from "svelte/transition";
    import Notification from "./Notification.svelte";

    let notifications = [];

    function addNotification(title, content, severity) {
        const id = Date.now();
        notifications = [{ id: id, title: title, content: content, severity: severity }, ...notifications];
        setTimeout(() => {
            notifications = notifications.filter(n => n.id != id);
        }, 3000);
    }

    try {
        events.on("notification", event => {
            addNotification(event.getTitle(), event.getMessage(), event.getSeverity());
        });
    } catch (err) {
        console.log(err);
    }
</script>

<div class="notifications">
    {#each notifications as n (n)}
        <div animate:flip={{ duration: 200 }} transition:fly={{ x: 15, duration: 200 }}>
            <Notification title={n.title} content={n.content} severity={n.severity} />
        </div>
    {/each}
</div>

<style>
    .notifications {
        position: absolute;
        bottom: 15px;
        right: 15px;
        display: grid;
        grid-auto-rows: max-content;
        row-gap: 10px;
    }
</style>
