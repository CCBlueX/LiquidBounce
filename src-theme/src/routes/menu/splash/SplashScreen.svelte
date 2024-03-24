<script lang="ts">
    import {fade} from "svelte/transition";
</script>

<img class="logo" src="img/lb-logo.svg" alt="logo" out:fade|global={{duration: 500}}>

<style lang="scss">
  .logo {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }
</style>
