<script lang="ts">
    import {onMount} from "svelte";

    onMount(() => {
        setTimeout(() => {
            const mainElement = document.querySelector("main");

            if (mainElement && mainElement.childElementCount > 0) {
                console.log("[Router] Detected route desync. Reloading...");
                window.location.replace(window.location.href.split("#").shift()!);
            }
        }, 500);
    });
</script>