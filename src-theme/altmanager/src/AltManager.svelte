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
</script>

<main>
    <div class="container">
        <div class="head">
            <img id="logo" src="img/lb.svg" alt="logo" class="logo" style="scale: 0.85">
            <div class="head-right">
                <div id="box">
                    <img src="https://i.imgur.com/0EdXK99.jpg" width="76px" alt="avatar" class="avatar">
                    <a href="#">
                        <img src="https://upload.wikimedia.org/wikipedia/en/thumb/b/ba/Flag_of_Germany.svg/1920px-Flag_of_Germany.svg.png" alt="flag" class="flag">
                        <span class="tooltip">Change Location</span>
                    </a>
                </div>
            </div>
        </div>
    </div>
    <div id="spacer"></div>
    <div class="container altmanager">
        <div class="head">
            <div class="container-sm" style="display: inherit">
                <label class="searchbox">
                    <img src="img/search.svg" width="20px" style="color: #ffffff;" alt="">
                    <input type="text" name="searchAlt" id="inputbox">
                </label>
            </div>
            <p id="test">Input Blablabla</p>
        </div>
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

<style>
    *:focus {
        outline: none;
    }

    * {
        cursor: default !important;
        font-family: "Montserrat", sans-serif;
        user-select: none;
        box-sizing: border-box;
        color: white;
    }

    body {
        background-image: url("img/background.png");
        background-size: cover;
        overflow: hidden;
        margin : 3rem;
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

    #logo  {
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

    .altmanager {
        display: flex;
        height: 100%;
        width: 98%;
        padding: 10px 2rem 10px 2rem;
        margin: auto;
        text-align: center;
        background: #3FB8AF;
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

    #textbox {
        width: 100%
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

    .butto:hover, .Button:hover {
        background-position: left bottom
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
        font-size: 26px;
    }


    Button {
        border-radius: 6px;
        padding: 5px;
        background-color: rgba(0, 0, 0, 0.68);
        width: 90px;
        height: 30px;
        color: white;
        border: none;
    }
</style>
