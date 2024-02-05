<script context="module">
    const BASE_URL = window.location.href.match(/^(http:\/\/[^\/]+)/)[1];
    console.log("BASE_URL: ", BASE_URL);
    // Format should be ws://localhost:10004/, only keep the host and port
    const BASE_WS_URL = BASE_URL.replace(/(http|https)/, "ws").replace(/\/$/, "")
    console.log("BASE_WS_URL: ", BASE_WS_URL);

    console.log("Connecting to server at: ", BASE_WS_URL)

    let ws = null;

    function connect() {
        ws = new WebSocket(BASE_WS_URL)
        ws.onopen = () => {
            console.log("Connected to server")
        }

        ws.onclose = () => {
            console.log("Disconnected from server, attempting to reconnect...")
            setTimeout(() => {
                connect();
            }, 1000)
        }

        ws.onerror = (error) => {
            console.error("WebSocket error: ", error)
        }

        ws.onmessage = (event) => {
            const json = JSON.parse(event.data);
            const eventName = json.name;
            const eventData = json.event;

            if (alwaysListeners[eventName]) {
                alwaysListeners[eventName].forEach(callback => {
                    callback(eventData)
                })
            }

            if (listeners[eventName]) {
                listeners[eventName].forEach(callback => {
                    callback(eventData)
                })
            }

        }
    }

    // List of event listener
    const alwaysListeners = {}
    let listeners = {}

    export function listenAlways(eventName, callback) {
        if (!alwaysListeners[eventName]) alwaysListeners[eventName] = []

        alwaysListeners[eventName].push(callback)
    }

    export function listen(eventName, callback) {
        if (!listeners[eventName]) listeners[eventName] = []

        listeners[eventName].push(callback)
    }

    export function cleanupListeners() {
        listeners = {}
    }

    // Send ping to server every 5 seconds
    setInterval(() => {
        if (!ws) return;
        if (ws.readyState !== 1) return;

        ws.send(JSON.stringify({
            name: "ping",
            event: {}
        }));
    }, 5000)

    connect();
</script>
