<script context="module">
    // Create websocket connection to server, if it disconnects it will try to reconnect

    // Remove anything after the last
    const BASE_API_URL = window.location.href
    // todo: use window location instead
    // Format should be ws://localhost:10004/, only keep the host and port
    const BASE_WS_URL = "ws://127.0.0.1:15743/"

    console.log("Connecting to server at: ", BASE_WS_URL)

    let ws = new WebSocket(BASE_WS_URL)
    ws.onopen = () => {
        console.log("Connected to server")
    }

    ws.onclose = () => {
        console.log("Disconnected from server, attempting to reconnect...")
        setTimeout(() => {
            ws = new WebSocket(BASE_WS_URL)
        }, 1000)
    }

    ws.onerror = (error) => {
        console.error("WebSocket error: ", error)
    }

    // List of event listener
    const listeners = {}

    export function listen(eventName, callback) {
        if (!listeners[eventName]) listeners[eventName] = []

        listeners[eventName].push(callback)
    }

    ws.onmessage = (event) => {
        const json = JSON.parse(event.data);
        const eventName = json.name;
        const eventData = json.event;

        if (!listeners[eventName]) return

        listeners[eventName].forEach(callback => {
            callback(eventData)
        })
    }
</script>
