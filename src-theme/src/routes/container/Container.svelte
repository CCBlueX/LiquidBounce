<script>
    import { sendChatMessage, sendCommand } from "../../client/api.svelte";

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
</script>

<style>
    main {
        margin-top: 20px;
        margin-left: 20px;
    }

    input {
        border-radius: 6px;
        margin-top: 10px;
        margin-bottom: 10px;
        padding: 5px;
        border-color: white;
        background-color: rgba(0, 0, 0, 0.68);
        color: white;
        border: none;
        height: 40px;
    }

    button {
        border-radius: 6px;
        padding: 5px;
        border-color: white;
        background-color: rgba(0, 0, 0, 0.68);
        color: white;
        border: none;
        height: 30px;
    }

    label {
        color: white;
    }
</style>

<main>
    <label>Write Chat Message</label><br>
    <input bind:value={chatMessage} placeholder="Chat Message"><br>
    <button on:click={handleSendMessage}>Send</button>
</main>
