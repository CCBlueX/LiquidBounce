<script lang="ts">
    import {fly} from "svelte/transition";
</script>

<div class="bottom-button-wrapper" in:fly|global={{duration: 180, y: 20}}>
    <slot/>
</div>

<style lang="scss">
  .bottom-button-wrapper {
    display: flex;
    justify-content: space-between;
  }
</style>
