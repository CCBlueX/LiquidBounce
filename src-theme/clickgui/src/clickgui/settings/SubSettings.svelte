<script>
    import {fade} from "svelte/transition";

    import GenericSetting from "./GenericSetting.svelte";

    export let settings;
</script>

<div class="settings" transition:fade|local={{duration: 200}}>
    {#each settings as s}
        <GenericSetting instance={s}/>
    {/each}
</div>

<style lang="scss">
  .settings {
    background-color: rgba(0, 0, 0, 0.36);
    border-left: solid 4px #4677FF;
    overflow: hidden;
  }
</style>
