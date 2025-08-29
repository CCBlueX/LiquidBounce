<script>

    import {fly} from "svelte/transition";

</script>
<div class="option-bar" transition:fly|global={{duration: 300, x: 1000}}>
    <slot/>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .option-bar {
    background-color: rgba($base, 0.5);
    padding: 15px 30px;
    display: flex;
    border-radius: 5px;
    align-items: center;
    column-gap: 30px;
    margin-bottom: 25px;
  }
</style>
