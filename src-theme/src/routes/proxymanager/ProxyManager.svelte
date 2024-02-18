<script>
    import {pop} from "svelte-spa-router"; // Ensure you have svelte-spa-router installed for this to work
    import {
        addProxy,
        getLocation,
        getProxies,
        getProxy,
        removeProxy,
        setProxy,
        unsetProxy
    } from "../../client/api.svelte";

    let proxies = [];
    let currentProxy = null;
    let currentLocation = 'Fetching Location...';
    let proxyInput = {
        host: "",
        port: "",
        username: "",
        password: ""
    };

    async function fetchProxies() {
        proxies = await getProxies();
    }

    async function fetchCurrentProxy() {
        const data = await getProxy();
        currentProxy = data ? `${data.host}:${data.port}` : "No Proxy";
    }

    async function fetchLocation() {
        const locationData = await getLocation();
        currentLocation = locationData ? locationData.country : 'Unknown Location';
    }

    const handleAddProxy = async () => {
        await addProxy(proxyInput);
        fetchProxies();
        proxyInput = {host: "", port: "", username: "", password: ""}; // reset input fields
    };

    const handleSetProxy = async (id) => {
        await setProxy(id);
        fetchCurrentProxy();
        fetchLocation();
    };

    const handleUnsetProxy = async () => {
        await unsetProxy();
        fetchCurrentProxy();
        fetchLocation();
    };

    const handleRemoveProxy = async (id) => {
        await removeProxy(id);
        fetchProxies();
    };

    fetchProxies();
    fetchCurrentProxy();
    fetchLocation();
</script>

<style>
    * {
        box-sizing: border-box;
        font-family: 'Montserrat', sans-serif;
    }

    main {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 20px;
        color: white;
    }

    h1 {
        color: white;
    }

    .proxy-list, .current-proxy, .add-proxy-form {
        width: 80%;
        max-width: 600px;
        margin-top: 20px;
        background-color: rgba(0, 0, 0, 0.68);
        border-radius: 6px;
        padding: 20px;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    input, button {
        border-radius: 6px;
        padding: 10px;
        border: none;
        margin-top: 5px;
    }

    input {
        width: 100%;
        border-radius: 6px;
        margin-top: 10px;
        margin-bottom: 5px;
        padding: 5px;
        border-color: white;
        background-color: rgba(0, 0, 0, 0.68);
        color: white;
        border: none;
        height: 40px;
    }

    button {
        margin-top: 10px;
        background-color: #4677ff;
        color: white;
        cursor: pointer;
    }

    .description {
        text-align: center;
        font-size: 14px;
        margin-bottom: 20px;
    }

    .current-proxy, .add-proxy-form {
        text-align: center;
    }

    table {
        width: 100%;
        border-collapse: collapse;
    }

    th, td {
        text-align: left;
        padding: 8px;
    }

    th {
        background-color: #4677ff;
        color: white;
    }

    .back-button {
        margin-top: 20px;
        padding: 10px 20px;
        font-size: 16px;
        background-color: #4677ff;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
    }
</style>

<main>
    <h1>Proxy Manager</h1>
    <p class="description">
        ProxyManager only supports SOCKS5 proxies. <br>
        This is a prototype design for testing purposes and is NOT the final design.
    </p>

    <div class="proxy-list">
        <h2>Proxies</h2>
        <table>
            <tr>
                <th>Host</th>
                <th>Port</th>
                <th>Auth</th>
                <th>Actions</th>
            </tr>
            <tr>
                <td colspan="3">None</td>
                <td><button on:click={() => handleUnsetProxy()}>Set</button></td>
            </tr>
            {#each proxies as proxy (proxy.id)}
                <tr>
                    <td>{proxy.host}</td>
                    <td>{proxy.port}</td>
                    {#if proxy.username.length > 0}
                        <td>&#128274;</td>
                    {:else}
                        <td>&#128275;</td>
                    {/if}

                    <td>
                        <button on:click={() => handleSetProxy(proxy.id)}>Set</button>
                        <button on:click={() => handleRemoveProxy(proxy.id)}>Remove</button>
                    </td>
                </tr>
            {/each}
        </table>
    </div>

    <div class="add-proxy-form">
        <h2>Add New Proxy</h2>
        <form on:submit|preventDefault={handleAddProxy}>
            <input type="text" bind:value={proxyInput.host} placeholder="Host"/>
            <input type="text" bind:value={proxyInput.port} placeholder="Port"/>
            <input type="text" bind:value={proxyInput.username} placeholder="Username"/>
            <input type="password" bind:value={proxyInput.password} placeholder="Password"/>
            <button type="submit">Add Proxy</button>
        </form>
    </div>

    <div class="current-proxy">
        <h2>Current Proxy</h2>
        <p>{currentProxy ? currentProxy : 'No active proxy'}</p>
        <p>Location: {currentLocation}</p>
    </div>

    <button class="back-button" on:click={pop}>Back</button>
</main>
