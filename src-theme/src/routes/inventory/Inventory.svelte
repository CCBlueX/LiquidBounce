<script>
    import {fade} from 'svelte/transition';
    import InventoryManagerProgressBar from "./InventoryManagerProgressBar.svelte";
</script>

<InventoryManagerProgressBar/>

<img class="watermark" src="img/lb-logo.svg" alt="watermark"
     in:fade={{ duration: 100 }} out:fade={{ duration: 100 }}>

<style>
    .watermark {
        position: absolute;
        bottom: 15px;
        right: 15px;
        width: 165px;
        opacity: 0.6;
    }
</style>
