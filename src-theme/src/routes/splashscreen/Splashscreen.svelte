<script>
    import {listen} from "../../client/ws.svelte";
    import {fade} from "svelte/transition";

    listen("splashProgress", (data) => {
        const isComplete = data.isComplete;

        if (isComplete) {
            let logo = document.getElementById("logo");
            logo.classList.add("fade-out");
        }
    });
</script>

<head>
    <style>
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
            animation: fade 2s forwards;
        }

        @keyframes fade {
            0% {
                opacity: 100;
            }
            100% {
                opacity: 0;
            }
        }
    </style>
</head>

<body transition:fade>
    <div class="center">
        <img id="logo" src="img/logo.svg" alt="logo" class="flash"><br>
    </div>
</body>
