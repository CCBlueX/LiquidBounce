<script>
   export let alt;
</script>

<div class="container-sm" style="display: inherit">
    <label class="searchbox">
        <img src="img/search.svg" width="20px" style="color: #ffffff;" alt="">
        <input type="text" name="searchAlt" id="inputbox">
    </label>
</div>

<style>
    .container-sm {
        padding: 1rem;
        border-radius: 5px;
        background: rgba(0,0,0);
        border: thick none rgb(70, 119, 255);
        border-bottom-style: solid;
        width: 40vw;
    }

    .searchbox {
        float: left;
        display: flex;
        justify-content: flex-start;
        align-items: center;
        gap: 1em;
        width: 100%;
    }

    #inputbox {
        background: none;
        border: none;
        width: 100%;
        height: 100%;
        font-size: 1rem;
    }
</style>
