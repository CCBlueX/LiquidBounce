<script>
    import { listen } from "../../client/ws.svelte";
    import {fade} from "svelte/transition";

    listen("splashProgress", (data) => {
        console.log(data);

        // from 0.0 to 1.0
        const progress = data.progress;
        const isComplete = data.isComplete;
        console.log("Splashscreen progress: " + data.progress);

        const progressElement = document.getElementById("progress");
        progressElement.value = progress * 100;

        if (isComplete) {
            document.getElementById("background")
                .classList.add("background-fade-out");

            let logo = document.getElementById("logo");
            logo.classList.add("fade-out");
            progressElement.classList.add("fade-out");
        }
    });
</script>

<head>
    <style>
        body {
            background-color: #181a1b;
        }

        .center {
            position: absolute;
            top: 50%;
            left: 50%;
            margin-right: -50%;
            transform: translate(-50%, -50%);
        }

        .flash {
            animation: flash 2s linear infinite;
        }

        @keyframes flash {
            50% {
                opacity: 0;
            }
        }

        .fade-out {
            animation: fade 1.5s forwards;
        }

        .background-fade-out {
            animation: color-fade 0.5s forwards;
        }

        @keyframes color-fade {
            0% {
                background-color: #1e1e1e;
            }
            100% {
                background-color: #1e1e1e00;
            }
        }

        @keyframes fade {
            0% {
                opacity: 100;
            }
            100% {
                opacity: 0;
            }
        }

        progress {
            margin-top: 20px;
            width: 275px;
            height: 20px;
        }
    </style>
</head>

<body id="background" transition:fade>
<div class="center">
    <img id="logo" src="img/logo.svg" alt="logo" class="flash"><br>

    <progress id="progress" value="0" max="100"></progress>
</div>
</body>
