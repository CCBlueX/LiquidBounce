<script>

</script>

<main>
    <div class="scale">
        <div class="wrapper">
            <h1>Better luck next time. This screen is empty!</h1>

            <button onclick="pages.open('title', screen)">Back</button>
        </div>
    </div>
</main>

<style>
    main {
        height: 100vh;
        width: 100vw;
        background-image: url("../img/background.png");
        background-size: cover;
        -webkit-user-select: none;
        cursor: default !important;
    }

    button {
        height: 75px;
        width: 150px;
        font-size: 26px;
        border-radius: 6px;
        margin-right: 10px;
        background-size: 200% 100%;
        background: rgba(0, 0, 0, 0.68) linear-gradient(to left, rgba(0, 0, 0, .68) 50%, #4677ff 50%) right bottom;
        will-change: background-position;
        transition: background-position .2s ease-out;
    }

    button:hover {
        background-position: left bottom;
    }

    .wrapper {
        position: relative;
        height: 100%;
    }

    .scale {
        position: relative;
        height: 100%;
        padding: 50px;
    }

    @media screen and (max-width: 1366px) {
        .scale {
            zoom: .7;
        }
    }

    @media screen and (max-width: 1024px) {
        .scale {
            zoom: .5;
        }
    }

    @media screen and (max-height: 1000px) {
        .scale {
            zoom: .7;
        }
    }

    @media screen and (max-height: 700px) {
        .scale {
            zoom: .5;
        }
    }

    @media screen and (max-height: 540px) {
        .scale {
            zoom: .4;
        }
    }
</style>
