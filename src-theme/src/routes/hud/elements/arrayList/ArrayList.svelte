<script lang="ts">
    import {fade} from 'svelte/transition';
    import {expoInOut} from 'svelte/easing';
    import Modules from "./Modules.svelte";

</script>

<div class="arraylist" id="arraylist"
     transition:fade|global={{ duration: 500,  easing: expoInOut }}
>
        <Modules/>
</div>

<style lang="scss">
  @import '../../../../colors';

  :root {
    --primary-color-rgb: var(--primary-color-rgb);
    --secondary-color-rgb: var(--secondary-color-rgb);
  }

  .arraylist {
    position: fixed;
    top: 0;
    right: 0;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    pointer-events: none;
    transform: translateZ(0);
  }

</style>
