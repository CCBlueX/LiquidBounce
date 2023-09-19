<script>
    import {createEventDispatcher} from "svelte";
    import ToolTip from "../ToolTip.svelte";

    export let text;
    export let icon;

    const dispatch = createEventDispatcher();

    function handleClick(e) {
        dispatch("click", e);
    }
</script>

<div class="button" on:click={handleClick}>
    <ToolTip {text}/>

    <div class="icon">
        <img src="img/icons/{icon}.svg" alt="icon">
    </div>
</div>

<style>
    .button {
        position: relative;
        margin: 0 10px;
    }

    .icon {
        height: 58px;
        width: 58px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
</style>
