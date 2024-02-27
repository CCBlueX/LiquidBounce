<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let title: string;

    const dispatch = createEventDispatcher();
</script>

<button class="button-setting" type="button" on:click={() => dispatch("click")}>{title}</button>

<style lang="scss">
    @import "../../../../colors.scss";

    .button-setting {
      border: none;
      background-color: $accent-color;
      color: $menu-text-color;
      font-family: "Inter", sans-serif;
      padding: 20px;
      border-radius: 5px;
      font-size: 20px;
      cursor: pointer;
      transition: ease background-color .2s;
      margin: 0 30px;

      &:hover {
        background-color: darken(desaturate($accent-color, 30%), 10%);
      }
    }
</style>