<script lang="ts">
    import RippleLoader from "../../RippleLoader.svelte";

    export let url: string;

    let previewImageLoaded = false;
</script>

<div class="avatar-wrapper">
    {#if !previewImageLoaded}
        <div class="loader">
            <RippleLoader />
        </div>
    {/if}

    <img class="avatar" on:load={() => previewImageLoaded = true} src={url} alt="avatar">
</div>

<style lang="scss">
  .avatar-wrapper {
    grid-area: a;
    height: 50px;
    width: 50px;
    position: relative;
  }

  .avatar {
    height: 100%;
    width: 100%;
    border-radius: 50%;
  }

  .loader {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }
</style>