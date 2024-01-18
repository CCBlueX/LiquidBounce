<script>
    import { reconnect, reconnectWithRandomAccount, reconnectWithRandomUsername } from "../../client/api.svelte";
</script>

<style>
    main {
        margin-top: 20px;
        margin-left: 20px;
    }

    button {
        border-radius: 6px;
        padding: 5px;
        border-color: white;
        background-color: rgba(0, 0, 0, 0.68);
        color: white;
        border: none;
        height: 30px;
        margin-bottom: 5px;
    }
</style>

<main>
    <button on:click={reconnect}>Reconnect</button><br>
    <button on:click={reconnectWithRandomAccount}>Reconnect with random account</button><br>
    <button on:click={reconnectWithRandomUsername}>Reconnect with random username</button>
</main>
