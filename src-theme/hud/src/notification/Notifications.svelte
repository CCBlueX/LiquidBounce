<script>
    import {flip} from "svelte/animate";
    import {fly} from "svelte/transition";
    import Notification from "./Notification.svelte";

    export let listen;

    let notifications = [];

    function addNotification(title, content, severity) {
        const id = Date.now();
        notifications = [{id: id, title: title, content: content, severity: severity}, ...notifications];
        setTimeout(() => {
            notifications = notifications.filter(n => n.id != id);
        }, 3000);
    }

    try {
        listen("notification", event => {
            let title = event.title;
            let message = event.message;
            let severity = event.severity;

            addNotification(title, message, severity);
        });
    } catch (err) {
        console.log(err);
    }
</script>

<div class="notifications">
    {#each notifications as n (n)}
        <div animate:flip={{ duration: 200 }} transition:fly={{ x: 15, duration: 200 }}>
            <Notification title={n.title} content={n.content} severity={n.severity}/>
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
