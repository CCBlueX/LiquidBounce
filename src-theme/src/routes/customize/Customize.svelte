<script>
    import {pop} from "svelte-spa-router";
    import {fade} from "svelte/transition";
</script>

<head>
    <meta charset="UTF-8">
    <title>Customize</title>
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
        <h1 id="title">Customize</h1>

        <p>No options yet.</p>
    </div>
    <footer>
        <div class="footinit">
            <button class="back" on:click={pop}>Back</button>
        </div>
    </footer>
</div>

</body>
