<script>

    import SearchBar from "./Searchbar.svelte";

    let selectedAlt = [];
    let selectedOptions = [];

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

    let favourite = true;
    let premium = true;

    function handleValueChange() {
        console.log("Test" + favourite);
    }

    console.log("Test 1243");

    function setRandomUsername() {
        document.getElementById('username').value = random();
    }

    function successMicrosoft(microsoftAccount) {
        console.log("Logged in with Microsoft");
        document.getElementById('response').innerText = "Logged in with Microsoft";
    }

    function errorMicrosoft(error) {
        console.log(error);
        document.getElementById('response').innerText = error;
    }

    function updateAccountList() {
        let accountsDiv = document.getElementById('accounts');
        let accounts = client.getAccountManager().getAccounts().iterator();

        // Clear the div
        accountsDiv.innerHTML = "";

        // Add all accounts
        console.log(accounts);
        var i = 0;
        while (accounts.hasNext()) {
            let account = accounts.next();
            console.log(JSON.stringify(account));
            console.log(account.getType());
            console.log(account.getName());

            const tblBody = document.createElement("tbody");
            //const img2 = document.createElement("img");
            const name = document.createTextNode(account.getName().toString());
            const td1 = document.createElement('td');
            const tr1 = document.createElement('tr');

            const faceUrl = client.getSessionService().getFaceUrl();
            //img2.src = faceUrl;
            //img2.alt = "failed";

            //div2.appendChild(img2);
            td1.appendChild(name);
            tr1.appendChild(td1);
            tblBody.appendChild(tr1);
            accountsDiv.appendChild(tblBody);
            i++;
        }
    }

    function updateAccountData() {
        const username = client.getSessionService().getUsername();
        const faceUrl = client.getSessionService().getFaceUrl();
        const accountType = client.getSessionService().getAccountType();
        const location = client.getSessionService().getLocation();

        //document.getElementById('sessionUsername').innerText = username;
        document.getElementById('face').src = faceUrl;
        document.getElementById('accountType').innerText = accountType;
        document.getElementById('location').innerText = location;
    }

    window.onload = function () {
        updateAccountList();
        updateAccountData();

        // Set altening token
        console.log(client.getAccountManager().getAlteningApiToken());
        document.getElementById('apitoken').value = client.getAccountManager().getAlteningApiToken();
    };

    function newCrackedAccount() {
        let username = document.getElementById('username').value;

        if (username === "") {
            document.getElementById('response').innerText = "Please enter a username";
            return;
        }

        client.getAccountManager().newCrackedAccount(username);
        updateAccountList();
    }

    function newAlteningAccount() {
        let token = document.getElementById('token').value;

        if (token === "") {
            document.getElementById('response').innerText = "Please enter a token";
            return;
        }

        client.getAccountManager().newAlteningAccount(token);
        updateAccountList();
    }

    function newMicrosoftAccount() {
        client.getAccountManager().newMicrosoftAccount(successMicrosoft, errorMicrosoft);
        updateAccountList();
    }

    function generateAlteningAccount() {
        let apiToken = document.getElementById('apitoken').value;

        if (apiToken === "") {
            document.getElementById('response').innerText = "Please enter an API token";
            return;
        }

        client.getAccountManager().setAlteningApiToken(apiToken);
        console.log("Generating new altening account using token: " + client.getAccountManager().getAlteningApiToken());
        client.getAccountManager().generateNewAlteningAccount(apiToken);
        updateAccountList();
    }

    function loginAccount(id) {
        client.getAccountManager().loginAccount(parseInt(id));
        updateAccountData();
    }

    function deleteAccount(id) {
        var account = client.getAccountManager().getAccounts().remove(parseInt(id));
        updateAccountList();

        document.getElementById('response').innerText = "Deleted account: " + account.getName();
    }
</script>

<main>
    <div class="container">
        <div class="head">
            <img id="logo" src="img/lb.svg" alt="logo" class="logo" style="scale: 0.85">
            <div class="head-right">
                <div id="box">
                    <img src="https://i.imgur.com/0EdXK99.jpg" width="76px" alt="avatar" class="avatar">
                    <a href="#">
                        <img src="https://upload.wikimedia.org/wikipedia/en/thumb/b/ba/Flag_of_Germany.svg/1920px-Flag_of_Germany.svg.png"
                             alt="flag" class="flag">
                        <span class="tooltip">Change Location</span>
                    </a>
                </div>
            </div>
        </div>
    </div>
    <div id="spacer"></div>
    <div class="footinit3">
        <div class="head">
            <SearchBar
                    placeholder="Search"
                    on:input={e => {
                        selectedOptions = [];
                        selectedAlt = [];
                        for (let i = 0; i < 10; i++) {
                            selectedAlt.push(random());
                            selectedOptions.push(random());
                        }
                    }}
            />
            <label class="switch">
                <input type="checkbox" bind:checked={favourite} on:change={handleValueChange}/>
                <span class="slider"/>
                <div class="name">{"Favouritesonly"}</div>
            </label>
            <label class="switch">
                <input type="checkbox" bind:checked={premium} on:change={handleValueChange}/>
                <span class="slider"/>
                <div class="name">{"PremiumOnly"}</div>
            </label>
            <p id="test">Input Blablabla</p>
        </div>
    </div>
    <div class="footinit4">
        <table id="accounts">
        </table>
    </div>
    <footer>
        <div class="footinit">
            <button class="butto" onclick="pages.open('title', screen)">
                <picture>
                    <source srcset="img/back.svg"
                            media="(orientation: portrait)">
                    <img src="img/back.svg" alt="">
                </picture>
                Back
            </button>
        </div>
        <div class="footinit2">
            <button class="butto" onclick="pages.open('title', screen)">
                <picture>
                    <source srcset="img/add.svg"
                            media="(orientation: portrait)">
                    <img src="img/add.svg" alt="">
                </picture>
                Add
            </button>
            <button class="butto" onclick="pages.open('title', screen)">
                <picture>
                    <source srcset="img/clipboard.svg"
                            media="(orientation: portrait)">
                    <img src="img/clipboard.svg" alt="">
                </picture>
                Clipboard
            </button>
            <button class="butto" onclick="pages.open('title', screen)">
                <picture>
                    <source srcset="img/import.svg"
                            media="(orientation: portrait)">
                    <img src="img/import.svg" alt="">
                </picture>
                Import
            </button>
            <button class="butto" onclick="pages.open('title', screen)">
                <picture>
                    <source srcset="img/random.svg"
                            media="(orientation: portrait)">
                    <img src="img/random.svg" alt="">
                </picture>
                Random
            </button>
            <button class="butto" onclick="pages.open('title', screen)">
                <picture>
                    <source srcset="img/check.svg"
                            media="(orientation: portrait)">
                    <img src="img/check.svg" alt="">
                </picture>
                Check
            </button>
        </div>
    </footer>
</main>

<style lang="scss">
  .name {
    font-weight: 200;
    color: white;
    font-size: 20px;
    margin-top: 10px;
    margin-left: 50px
  }

  td {
    padding: 0.5rem;
    height: 45px;
    width: 155px;
    font-size: 22px;
    background-color: rgba(0, 0, 0, 20.68);
    border-radius: 6px;
    margin-right: 10px;
    background: linear-gradient(to left, rgba(0, 0, 0, 10.68) 50%, #4677ff 50%);
    background-size: 200% 100%;
    background-position: right bottom;
    will-change: background-position;
    transition: background-position .2s ease-out;
  }

  table {
    margin: 1rem auto;
  }

  .slider {
    width: 28px;
    position: absolute;
    top: 18px;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #707070;
    transition: ease 0.4s;
    height: 14px;
    border-radius: 14px;

    &::before {
      position: absolute;
      content: "";
      height: 20px;
      width: 20px;
      top: -3px;
      left: -6px;
      background-color: white;
      transition: ease 0.4s;
      border-radius: 50%
    }
  }

  .switch {
    position: relative;
    display: inline-block;
    width: 22px;
    height: 12px
  }

  .switch input {
    display: none;
  }

  .switch input:checked + .slider {
    background-color: #4860a7;
  }

  .switch input:checked + .slider:before {
    transform: translateX(22px);
    background-color: #4677ff;
  }

  .container {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
  }

  .head {
    width: 100%;
    height: 100%;
    text-align: center;
    display: flex;
    justify-content: space-between;
  }

  #logo {
    float: left;
  }

  .head-right {
    float: right;
    display: flex;
    width: 9rem;
  }

  #box {
    width: 100%;
    height: 100%;
    background: rgba(10, 10, 10, 0.62);
    text-align: center;
    padding: 0.8rem;
  }

  #box a span {
    position: absolute;
    display: inline-block;
    border-bottom: 1px dotted black;
    color: white;
    background: #000000;
    z-index: 1;
    visibility: hidden;
    top: 1em;
    left: 109.5em;
    padding: 10px;
    border-radius: 30px;
  }

  #box a:hover span {
    visibility: visible;
  }

  #test {
    float: right;
  }

  #spacer {
    width: 100%;
    height: 10vh;
  }

  .avatar {
    border-radius: 50px;
    box-shadow: 3px 3px 6px #000;
  }

  .flag {
    width: 1.3rem;
    height: 1.3rem;
    border-radius: 50%;
    margin-left: -1.6em;
    margin-bottom: 3.5em;
    box-shadow: 3px 3px 6px #000
  }

  .footinit {
    background-color: rgba(0, 0, 0, 0.68);
    border-radius: 6px;
    align-items: center;
    column-gap: 30px;
    font-size: 26px;
    padding: 15px 35px;
    position: absolute;
    bottom: 20px;
    text-align: right;
    right: 20px;
    margin-bottom: 10px;
  }

  .footinit3 {
    background-color: rgba(0, 0, 0, 0.68);
    width: 98%;
    border-radius: 6px;
    align-items: center;
    column-gap: 30px;
    font-size: 26px;
    padding: 15px 35px;
    position: absolute;
    top: 17%;
    text-align: right;
    left: 20px;
    margin-top: 10px
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
    row-gap: 5px;
    column-gap: 30px;
  }

  .footinit4 {
    background-color: rgba(0, 0, 0, 0.68);
    width: 90%;
    height: 55%;
    border-radius: 6px;
    align-items: center;
    column-gap: 30px;
    font-size: 26px;
    padding: 15px 35px;
    position: absolute;
    top: 31%;
    text-align: right;
    left: 20px;
    margin-top: 10px
  }

  .footinit2 {
    background-color: rgba(0, 0, 0, 0.68);
    border-radius: 6px;
    align-items: center;
    column-gap: 30px;
    font-size: 26px;
    padding: 15px 35px;
    position: absolute;
    bottom: 20px;
    text-align: left;
    left: 20px;
    margin-bottom: 10px;
  }

  .butto {
    height: 45px;
    width: 155px;
    font-size: 22px;
    background-color: rgba(0, 0, 0, 0.68);
    border-radius: 6px;
    margin-right: 10px;
    background: linear-gradient(to left, rgba(0, 0, 0, .68) 50%, #4677ff 50%);
    background-size: 200% 100%;
    background-position: right bottom;
    will-change: background-position;
    transition: background-position .2s ease-out;
  }

  .butto2 {
    height: 45px;
    width: 105px;
    font-size: 22px;
    background-color: rgba(0, 0, 0, 10.68);
    border-radius: 6px;
    margin-right: 10px;
    background: linear-gradient(to left, rgba(0, 0, 0, 10.68) 50%, #4677ff 50%);
    background-size: 200% 100%;
    background-position: right bottom;
    will-change: background-position;
    transition: background-position .2s ease-out;
  }

  .butto:hover {
    background-position: left bottom
  }

  .butto2:hover {
    background-position: left bottom
  }
</style>
