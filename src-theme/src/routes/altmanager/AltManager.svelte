<script>
    import {
        deleteAccount,
        getAccounts,
        getLocation,
        getSession,
        loginAccount,
        loginCrackedAccount,
        newAltening,
        newAlteningGen,
        newCrackedAccount,
        newMicrosoftAccount,
        newMicrosoftAccountClipboard,
        restoreInitialAccount
    } from "../../client/api.svelte";
    import {listen} from "../../client/ws.svelte";
    import {pop} from "svelte-spa-router";
    import {fade} from "svelte/transition";

    const random = (length = 8) => {
        // Declare all characters
        let chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

        // Pick characers randomly
        let str = '';
        for (let i = 0; i < length; i++) {
            str += chars.charAt(Math.floor(Math.random() * chars.length));
        }

        return str;

    };

    let crackedUsername = random();

    function setRandomUsername() {
        crackedUsername = random();
    }

    let accounts = [];

    function altManagerUpdate(event) {
        let message = event.message;
        let success = event.success;

        feedback(message, success ? "green" : "red")

        updateAccountData();
        updateAccountList();
    }

    function updateAccountList() {
        getAccounts().then(list => {
            accounts = list.map((account, index) => {
                return {
                    index: index,
                    account: account
                }
            });
        }).catch(console.error);
    }

    let sessionUsername = "Loading...";
    let accountType = "Loading...";
    let avatar = "";
    let location = "Loading...";

    function updateAccountData() {
        getSession().then(session => {
            sessionUsername = session.username;
            accountType = session.accountType;
            avatar = session.avatar;
        }).catch(console.error);

        getLocation().then(ip => {
            // Lowercase country code
            location = ip.country;
        }).catch(console.error);
    }

    function siteNewCrackedAccount() {
        if (crackedUsername.trim() === "") {
            feedback("Please enter a username", "red");
            return;
        }

        newCrackedAccount(crackedUsername);
    }

    function siteLoginCrackedAccount() {
        if (crackedUsername.trim() === "") {
            feedback("Please enter a username", "red");
            return;
        }

        loginCrackedAccount(crackedUsername);
    }

    function siteLoginRandomCrackedAccount() {
        crackedUsername = random();
        loginCrackedAccount(crackedUsername);
    }
    
    let alteningAccountToken = "";

    function siteNewAlteningAccount() {
        if (alteningAccountToken.trim() === "") {
            feedback("Please enter an account token", "red");
            return;
        }

        feedback("Adding account...", "white");
        newAltening(alteningAccountToken);
    }

    let alteningApiToken = localStorage.getItem("alteningApiToken") || "";

    function siteGenerateAlteningAccount() {
        if (alteningApiToken.trim() === "") {
            feedback("Please enter an API token", "red");
            return;
        }

        localStorage.setItem("alteningApiToken", alteningApiToken);

        feedback("Generating account...", "white");
        newAlteningGen(alteningApiToken);
    }

    function siteLoginAccount(id) {
        feedback("Logging in...", "white")

        loginAccount(id)
            .catch(error => {
                feedback(error, "red");
            });
    }

    function siteDeleteAccount(id) {
        deleteAccount(id).then(account => {
            updateAccountList();
            feedback("Deleted account " + account.username + "!", "green");
        }).catch(error => {
            feedback(error, "red");
        });

    }

    function siteRestoreInitial() {
        restoreInitialAccount().then(() => {
            updateAccountData();
            updateAccountList();
            feedback("Restored initial account!", "green");
        }).catch(error => {
            feedback(error, "red");
        });
    }

    let statusMessage = "Idle...";
    let statusColor = "white";

    function feedback(message, color) {
        statusMessage = message;
        statusColor = color;
    }

    listen("altManagerUpdate", altManagerUpdate);
    updateAccountList();
    updateAccountData();
</script>


<head>
    <meta charset="UTF-8">
    <title>Alt Manager</title>
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
            row-gap: 5px;
        }

        .button {
            background: linear-gradient(to left, rgba(0, 0, 0, .68) 50%, #4677ff 50%);
            background-size: 200% 100%;
            background-position: right bottom;
            will-change: background-position;
            transition: background-position .2s ease-out;
            margin-top: 10px;
            width: 23%;
            padding: 25px 35px;
            border-radius: 6px;
            align-items: center;
            column-gap: 30px;
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
            margin-right: 10px;
            padding: 5px;
            background-color: rgba(0, 0, 0, 0.68);
            min-width: 80px;
            height: 30px;
            color: white;
            border: none;
        }

        td {
            justify-content: center;
            text-align: center;
            padding: 10px;
        }

        .back {
            position: absolute;
            bottom: 20px;
            right: 20px;

            height: 50px;
            width: 150px;

            font-size: 26px;

            border-radius: 6px;
            margin-right: 10px;

            background-color: rgba(0, 0, 0, 0.68);
            background: linear-gradient(to left, rgba(0, 0, 0, .68) 50%, #4677ff 50%);
            background-size: 200% 100%;
            background-position: right bottom;

            will-change: background-position;
            transition: background-position .2s ease-out;
        }

        .back:hover, .button:hover {
            background-position: left bottom;
        }

        .accounts {
            position: absolute;
            top: 0;
            right: 0;
            width: 75%;
            height: 85%;
            background-color: rgba(0, 0, 0, 0.68);
            border-radius: 6px;
            padding: 20px;
            overflow-y: scroll;
        }

        #face {
            width: 32px;
            height: 32px;
        }

        #status {
            text-align: right;
        }
    </style>
</head>
<body transition:fade>
<div class="scale">
    <div class="wrapper">
        <h1 id="title">Alt Manager</h1>
        <b>This is a prototype design for testing purposes <br>and is NOT the final design.</b>

        <div id="buttongrid">
            <div class="button">
                <img id="face" alt="head" class="head" src={avatar}/>
                <br>

                <label><b>Username:</b></label>
                <div id="sessionUsername" class="username"><p>{sessionUsername}</p></div>
                <label><b>Account Type:</b></label>
                <div id="accountType" class="accountType"><p>{accountType}</p></div>
                <label><b>Location:</b></label>
                <div id="location" class="location"><p>{location}</p></div>

                <br>
                <button on:click={siteRestoreInitial}>
                    Restore Initial
                </button>

                <br>
                <p class="status" id="status" style="color: {statusColor};">{statusMessage}</p>
            </div>

            <div class="button">
                <h2>Cracked Login</h2>
                <div>
                    <label for="username"></label>
                    <input type="text" id="username" name="username" placeholder="Username" bind:value={crackedUsername}>
                    <button on:click={setRandomUsername}>Random</button>
                    <br>
                    <button on:click={siteNewCrackedAccount}>
                        Add
                    </button>
                    <button on:click={siteLoginCrackedAccount}>
                        Login
                    </button>
                    <button on:click={siteLoginRandomCrackedAccount}>
                        Login Random
                    </button>

                </div>
            </div>

            <div class="button">
                <h2>Microsoft Login</h2>

                This will open your browser to sign in to your Microsoft account.<br>
                <br>

                <button on:click={newMicrosoftAccount}>
                    Browser
                </button>
                <button on:click={newMicrosoftAccountClipboard}>
                    Copy URL
                </button>
            </div>
            <div class="button">
                <h2>Altening Login</h2>

                <div>
                    <label for="token"></label><input type="text" id="token" name="token" placeholder="Account Token" bind:value={alteningAccountToken}>
                    <button on:click={siteNewAlteningAccount}>
                        Add
                    </button>
                    <br>
                    <label for="apitoken"></label>
                    <input type="password" id="apitoken" name="apitoken" placeholder="API Token" bind:value={alteningApiToken}>
                    <button on:click={siteGenerateAlteningAccount}>
                        Add
                    </button>
                </div>
            </div>

            <div class="accounts" id="accounts">
                <table>
                    <tr>
                        <th>Avatar</th>
                        <th>Type</th>
                        <th>Username</th>
                        <th>Actions</th>
                    </tr>

                    {#each accounts as entry}
                        <tr>
                            <td>
                                <img src={entry.account.avatar} width="20" height="20"
                                     alt="head" class="head"/>
                            </td>
                            <td>{entry.account.type}</td>
                            <td>{entry.account.username}</td>
                            <td>
                                <button on:click={siteLoginAccount(entry.index)}>
                                    Login
                                </button>
                                <button on:click={siteDeleteAccount(entry.index)}>
                                    Delete
                                </button>
                            </td>
                        </tr>
                    {/each}
                </table>


            </div>
        </div>
    </div>
    <footer>
        <button class="back" on:click={pop}>Back</button>
    </footer>
</div>
</body>

