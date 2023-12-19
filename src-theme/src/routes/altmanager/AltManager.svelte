<!DOCTYPE html>
<html lang="en">
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
            row-gap: 5px;
        }

        .wrapper {
            position: relative;
            height: 100%;
            grid-gap: 30px;
            margin: 60px;
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

        .footinit {
            display: block;
            position: absolute;
            bottom: 0;
            width: 100%;
            text-align: right;
            height: 100px;
            margin-bottom: 45px;
        }

        .butto {
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

        .butto:hover, .button:hover {
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
    <script>
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

        function setRandomUsername() {
            document.getElementById('username').value = random();
        }

        function altManagerUpdate(event) {
            let message = event.getMessage();
            let success = event.getSuccess();

            console.log(message);
            feedback(message, success ? "green" : "red")

            updateAccountData();
            updateAccountList();
        }

        function updateAccountList() {
            let accountsDiv = document.getElementById('accounts');
            let accounts = client.getAccountManager().getAccounts().iterator();

            // Clear the div
            accountsDiv.innerHTML = "";

            // Add all accounts
            let i = 0;
            console.log("Account List updating...")
            while (accounts.hasNext()) {
                let account = accounts.next();
                let accountProfile = account.getProfile();
                let accountUsername = accountProfile.getUsername();
                let accountType = account.getType();
                console.log("Account " + i + ": " + accountType + " " + accountUsername);

                let row = document.createElement('tr');
                let avatar = document.createElement('td');
                let type = document.createElement('td');
                let name = document.createElement('td');
                let button = document.createElement('td');

                // If account has UUID, set avatar by UUID
                // https://avatar.liquidbounce.net/avatar/<uuid>/32
                // Otherwise, set default https://avatar.liquidbounce.net/avatar/MHF_Steve

                if (accountProfile.getUuid() !== null) {
                    avatar.innerHTML = "<img src='https://avatar.liquidbounce.net/avatar/" + accountProfile.getUuid() + "/20' width='20' height='20' alt='head' class='head'/>";
                } else {
                    // fix: when username, it does not take size argument on API
                    avatar.innerHTML = "<img src='https://avatar.liquidbounce.net/avatar/" + accountUsername + "' width='20' height='20' alt='head' class='head'/>";
                }

                type.innerText = accountType;
                name.innerText = accountUsername;
                button.innerHTML = "<button onclick='loginAccount(\"" + i + "\")'>Login</button><button onclick='deleteAccount(\"" + i + "\")'>Delete</button><button onclick='copyAccount(\"" + i + "\")'>Copy</button><button onclick='favoriteAccount(\"" + i + "\")'>Favorite</button>";

                row.appendChild(avatar);
                row.appendChild(type);
                row.appendChild(name);
                row.appendChild(button);
                accountsDiv.appendChild(row);
                i++;
            }
        }

        function updateAccountData() {
            const username = client.getSessionService().getUsername();
            const faceUrl = client.getSessionService().getFaceUrl();
            const accountType = client.getSessionService().getAccountType();
            const location = client.getSessionService().getLocation();

            document.getElementById('sessionUsername').innerText = username;
            document.getElementById('face').src = faceUrl;
            document.getElementById('accountType').innerText = accountType;
            document.getElementById('location').innerText = location;
        }

        window.onload = function () {
            updateAccountList();
            updateAccountData();

            // Set altening token
            document.getElementById('apitoken').value = client.getAccountManager().getAlteningApiToken();
        };

        function newCrackedAccount() {
            let username = document.getElementById('username').value;

            if (username === "") {
                feedback("Please enter a username", "red");
                return;
            }

            client.getAccountManager().newCrackedAccount(username);
            updateAccountList();

            feedback("Account added!", "green");
        }

        function newAlteningAccount() {
            let token = document.getElementById('token').value;

            if (token === "") {
                feedback("Please enter an account token", "red");
                return;
            }

            feedback("Adding account...", "white");
            client.getAccountManager().newAlteningAccount(token);
            updateAccountList();
            feedback("Account added!", "green");
        }

        function newMicrosoftAccount() {
            client.getAccountManager().newMicrosoftAccount();
        }

        function generateAlteningAccount() {
            let apiToken = document.getElementById('apitoken').value;

            if (apiToken === "") {
                feedback("Please enter an API token", "red");
                return;
            }

            feedback("Generating account...", "white");
            client.getAccountManager().setAlteningApiToken(apiToken);
            client.getAccountManager().generateAlteningAccountAsync(apiToken);
        }

        function loginAccount(id) {
            feedback("Logging in...", "white")

            try {
                client.getAccountManager().loginAccountAsync(parseInt(id));
            } catch (e) {
                console.error(e);
                feedback("Failed to login: " + e.getMessage(), "red");
            }
        }

        function deleteAccount(id) {
            const account = client.getAccountManager().getAccounts().remove(parseInt(id));
            const accountProfile = account.getProfile();
            updateAccountList();

            feedback("Deleted account: " + accountProfile.getUsername(), "green");
        }

        function restoreInitial() {
            client.getAccountManager().restoreInitial();
            updateAccountData();
            feedback("Restored initial account", "green");
        }

        function feedback(message, color) {
            let status = document.getElementById('status');

            status.innerText = message;
            status.style.color = color;
        }

        try {
            events.on("altManagerUpdate", altManagerUpdate);
        } catch (e) {
            console.error(e);
        }
    </script>
</head>
<body>
<div class="scale">
    <div class="wrapper">
        <h1 id="title">Alt Manager</h1>
        <b>This is a prototype design for testing purposes <br>and is NOT the final design.</b>

        <div id="buttongrid">
            <div class="button">
                <img id="face" alt="head" class="head"/>
                <br>

                <label><b>Username:</b></label>
                <div id="sessionUsername" class="username"></div>
                <label><b>Account Type:</b></label>
                <div id="accountType" class="accountType"></div>
                <label><b>Location:</b></label>
                <div id="location" class="location"></div>

                <br>
                <button onclick="restoreInitial()">
                    Restore Initial
                </button>

                <br>
                <p class="status" id="status">Idle...</p>
            </div>

            <div class="button">
                <h2>Cracked Login</h2>
                <div>
                    <label for="username"></label>
                    <input type="text" id="username" name="username" placeholder="Username">
                    <button onclick="setRandomUsername()">Random</button>
                    <br>
                    <button onclick="newCrackedAccount()">
                        Add
                    </button>


                </div>
            </div>

            <div class="button">
                <h2>Microsoft Login</h2>

                This will open your browser to sign in to your Microsoft account.<br>
                <br>

                <button onclick="newMicrosoftAccount()">
                    Add
                </button>
            </div>
            <div class="button">
                <h2>Altening Login</h2>

                <div>
                    <label for="token"></label><input type="text" id="token" name="token" placeholder="Account Token">
                    <button onclick="newAlteningAccount()">
                        Add
                    </button>
                    <br>
                    <label for="apitoken"></label>
                    <input type="password" id="apitoken" name="apitoken" placeholder="API Token">
                    <button onclick="generateAlteningAccount()">
                        Add
                    </button>
                </div>
            </div>

            <div class="accounts" id="accounts"></div>
        </div>
    </div>
    <footer>
        <div class="footinit">
            <button class="butto" onclick="pages.open('title', screen)">Back</button>
        </div>
    </footer>
</div>
</body>
</html>
