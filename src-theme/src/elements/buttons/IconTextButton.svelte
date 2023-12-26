<script>
    import {createEventDispatcher} from "svelte";

    export let text;
    export let icon;

    const dispatch = createEventDispatcher();

    function handleClick(e) {
        dispatch("click", e);
    }
</script>

<div class="button" on:click={handleClick}>
    <div class="icon">
        <img src="img/icons/{icon}.svg" alt="icon">
    </div>
    <div class="text">{text}</div>
</div>

<style lang="scss">
  .button {
    display: grid;
    grid-template-columns: max-content auto max-content;
    border-radius: 6px;
    align-items: center;
    column-gap: 30px;
    overflow: hidden;
    background: linear-gradient(to left, rgba(0, 0, 0, .36) 50%, #4677ff 50%);
    background-size: 200% 100%;
    background-position: right bottom;
    will-change: background-position;
    transition: background-position .2s ease-out;
    margin: 0 10px;

    &:hover {
      background-position: left bottom;
    }
  }

  .icon {
    height: 58px;
    width: 58px;
    background-color: #4677FF;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .text {
    font-size: 20px;
    font-weight: medium;
    color: white;
  }
</style>
