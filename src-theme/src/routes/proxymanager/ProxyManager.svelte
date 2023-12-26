<script>
    import {pop} from "svelte-spa-router";
    import {getLocation, getProxy, setProxy, unsetProxy} from "../../client/api.svelte";
    import {fade} from "svelte/transition";

    let proxyHost = "";
    let proxyPort = 1080;
    let proxyUsername = "";
    let proxyPassword = "";

    let proxy = "";
    let location = "Loading...";
    let status = "";

    function set() {
        setProxy(proxyHost, proxyPort, proxyUsername, proxyPassword).then(() => {
            status = "Proxy set.";
            refreshLocation();
        }).catch((e) => {
            status = "Error: " + e;
            console.error(e);
        });
    }

    function remove() {
        unsetProxy().then(() => {
            status = "Proxy unset.";
            refreshLocation();
        }).catch((e) => {
            status = "Error: " + e;
            console.error(e);
        });
    }

    function fillInData() {
        getProxy().then((proxy) => {
            if (proxy.host === undefined || proxy.port === undefined) {
                return;
            }

            proxyHost = proxy.host;
            proxyPort = proxy.port;
            proxyUsername = proxy.username;
            proxyPassword = proxy.password;
        }).catch((e) => {
            status = "Error: " + e;
            console.error(e);
        });
    }

    function refreshLocation() {
        location = "Loading...";
        getProxy().then((p) => {
            if (p.host === undefined || p.port === undefined) {
                proxy = "None";
            } else {
                proxy = p.host + ":" + p.port;
            }
        }).catch((e) => {
            status = "Error: " + e;
            console.error(e);
        });

        getLocation().then(ip => {
            const country = ip.country;
            
            // Lowercase country code
            location = country;
        }).catch(console.error);
    }

    fillInData();
    refreshLocation();
</script>

<head>
    <meta charset="UTF-8">
    <title>Proxy Manager</title>
    <style>

        *:focus {
            outline: none;
        }

        * {
            cursor: default !important;
            font-family: "Montserrat", sans-serif;
            user-select: none;
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            color: white;
        }

        body {
            background-image: url("img/background.png");
            background-size: cover;
            height: 100vh;
            width: 100vw;
            overflow: hidden;
            margin: 0;
            padding: 0;
        }

        #title {
            text-align: left;
            font-size: 56px;
        }

        #buttongrid {
            display: grid;
            top: 150px;
            grid-auto-rows: max-content;
            row-gap: 20px;
        }

        .wrapper {
            position: relative;
            height: 100%;
            grid-gap: 30px;
            margin: 60px;
        }

        .Button {
            background: linear-gradient(to left, rgba(0, 0, 0, .68) 50%, #4677ff 50%);
            background-size: 200% 100%;
            background-position: right bottom;
            will-change: background-position;
            transition: background-position .2s ease-out;
            margin-top: 30px;
            width: 420px;
            padding: 25px 35px;
            border-radius: 6px;
            align-items: center;
            column-gap: 30px;
            font-size: 26px;
        }

        .scale {
            position: relative;
            height: 100%;
        }

        @media screen and (max-width: 1366px) {
            .scale {
                zoom: .5;
            }
        }

        @media screen and (max-width: 1024px) {
            .scale {
                zoom: .6;
            }
        }

        @media screen and (max-height: 1000px) {
            .scale {
                zoom: .9;
            }
        }

        @media screen and (max-height: 700px) {
            .scale {
                zoom: .6;
            }
        }

        @media screen and (max-height: 540px) {
            .scale {
                zoom: .5;
            }
        }

        Input {
            border-radius: 6px;
            margin-top: 10px;
            padding: 10px;
            border-color: white;
            background-color: rgba(0, 0, 0, 0.68);
            color: white;
            border: none;
            height: 40px;
            width: 100%;
        }

        Button {
            border-radius: 6px;
            padding: 5px;
            background-color: rgba(0, 0, 0, 0.68);
            width: 90px;
            height: 35px;
            color: white;
            border: none;
        }

        td {
            justify-content: center;
            text-align: center;
            padding: 10px;
        }

        .footinit {
            display: block;
            position: absolute;
            bottom: 0;
            width: 100%;
            text-align: right;
            height: 100px;
            margin-bottom: 45px;
        }

        .back {
            height: 75px;
            width: 150px;
            font-size: 26px;
            background-color: rgba(0, 0, 0, 0.68);
            border-radius: 6px;
            margin-right: 10px;
            background: linear-gradient(to left, rgba(0, 0, 0, .68) 50%, #4677ff 50%);
            background-size: 200% 100%;
            background-position: right bottom;
            will-change: background-position;
            transition: background-position .2s ease-out;
        }

        .back:hover, Button:hover {
            background-position: left bottom;
        }

    </style>
</head>
<body transition:fade>
<div class="scale">
    <div class="wrapper">
        <h1 id="title">Proxy Manager</h1>

        <div id="buttongrid">
            <div class="Button">
                <h2>Proxy Configuration</h2>
                <div>
                    <label for="host"></label><input type="text" id="host" name="host"
                                                     placeholder="Host" bind:value={proxyHost}><br>
                    <label for="port"></label><input type="port" id="port" name="port"
                                                     placeholder="Port" bind:value={proxyPort}><br>
                    <label for="username"></label><input type="text" id="username" name="username"
                                                         placeholder="Username" bind:value={proxyUsername}><br>
                    <label for="password"></label><input type="password" id="password" name="password"
                                                         placeholder="Password" bind:value={proxyPassword}>

                    <br><br>

                    <button on:click={set}>
                        Set proxy
                    </button>

                    <button on:click={remove}>
                        Unset proxy
                    </button>
                </div>
            </div>

            <label id="status">{status}</label>
        </div>

        <div>
            <h2>Proxy Details</h2>

            <label><b>Current proxy: </b><label id="proxy">{proxy}</label></label><br>
            <label><b>Location: </b><label id="location">{location}</label></label>
        </div>
    </div>
    <footer>
        <div class="footinit">
            <button class="back" on:click={pop}>Back</button>
        </div>
    </footer>
</div>

</body>
