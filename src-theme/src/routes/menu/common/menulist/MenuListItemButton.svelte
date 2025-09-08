<script lang="ts">
    import ToolTip from "../ToolTip.svelte";
    import {createEventDispatcher} from "svelte";

    export let title: string;
    export let icon: string;

    const dispatch = createEventDispatcher();
</script>

<button class="button" on:click={() => dispatch("click")} type="button">
    <ToolTip color="transparent" text={title}/>
    <img alt={icon} class="icon" src="img/menu/icon-{icon}.svg">
</button>

<style lang="scss">
  .button {
    background-color: transparent;
    border: none;
    cursor: pointer;
    position: relative;
    margin-left: 15px;
  }

  .icon {
    height: 27px;
    width: 27px;
  }
</style>
