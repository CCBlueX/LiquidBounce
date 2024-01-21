<script>
    import { getContainer, sendChatMessage, sendCommand, panic, giveContainer, storeContainer } from "../../client/api.svelte";

    let chatMessage = "";

    function handleSendMessage() {
        if (chatMessage.trim().length > 0) {
            if (chatMessage.startsWith("/")) {
                sendCommand(chatMessage.substring(1)).catch(console.log);
            } else {
                sendChatMessage(chatMessage).catch(console.log);
            }
        }
    }

    let syncId;
    let slots;
    let emptySlots;
    let rows;

    function updateContainerData() {
        getContainer().then(container => {
            syncId = container.syncId;
            slots = container.slots;
            emptySlots = container.emptySlots;
            rows = container.rows;
        }).catch(console.log);
    }

    updateContainerData();
</script>

<style>
    main {
        background-color: rgba(0, 0, 0, 0.4);
        border-radius: 8px;
        padding: 20px;
        margin: 20px;
        width: 250px;
    }

    input, button {
        height: 30px;
        border-radius: 6px;
        padding: 5px;
        background-color: rgba(0, 0, 0, 0.7);
        color: white;
        border: none white;
        margin-bottom: 10px;
    }

    button {
        width: 43%;
    }

    h2, h3, p {
        color: white;
    }
</style>

<main>
    <h2>Container Data</h2>

    <p>Sync ID: {syncId}</p>
    <p>Slots: {slots}</p>
    <p>Empty Slots: {emptySlots}</p>
    <p>Rows: {rows}</p><br>

    <button on:click={updateContainerData}>Update</button><br><br>

    <h2>Actions</h2>
    <button on:click={giveContainer}>Give</button>
    <button on:click={storeContainer}>Store</button>

    <br><br>
    <h2>Quick Actions</h2>
    <p>Write a chat message</p>

    <input bind:value={chatMessage} placeholder="Chat Message">
    <button on:click={handleSendMessage}>Send</button><br>


    <button on:click={panic}>Panic All</button>
</main>
