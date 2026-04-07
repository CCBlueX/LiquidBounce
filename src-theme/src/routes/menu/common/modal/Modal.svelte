<script lang="ts">
    import {fade, fly} from "svelte/transition";
    import {createEventDispatcher} from "svelte";

    export let title: string;
    export let visible: boolean;

    const dispatch = createEventDispatcher();

    function handleClick() {
        dispatch("close");
        visible = false;
    }
</script>

{#if visible}
    <div class="modal-wrapper" transition:fade|global={{duration: 200}}>
        <div class="modal" in:fly|global={{duration: 300, y: -100}} out:fly|global={{duration: 300, y: -100}}>
            <button class="button-modal-close" on:click={handleClick}>
                <img src="img/menu/icon-close.svg" alt="close">
            </button>

            <div class="title">{title}</div>

            <div class="content">
                <slot />
            </div>
        </div>
    </div>
{/if}

<style lang="scss">

  .modal-wrapper {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: var(--menu-modal-backdrop-color);
    z-index: 99999;
  }

  .modal {
    background-color: var(--menu-modal-background-color);
    min-width: 500px;
    position: fixed;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    padding: 40px;
    display: flex;
    flex-direction: column;
    border-radius: 5px;
    box-shadow: 0 0 10px var(--menu-modal-shadow-color);
  }

  .title {
    color: var(--menu-text-color);
    font-size: 34px;
    position: relative;
    width: max-content;
    align-self: center;
    margin-bottom: 80px;

    &::after {
      content: "";
      position: absolute;
      display: block;
      height: 8px;
      width: calc(90%);
      background-color: var(--menu-modal-title-accent-color);
      bottom: -25px;
      left: 50%;
      transform: translateX(-50%);
      border-radius: 10px;
    }
  }

  .content {
    display: flex;
    flex-direction: column;
    row-gap: 40px;
  }

  .button-modal-close {
    height: 40px;
    width: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: transparent;
    border: solid 2px var(--menu-modal-close-border-color);
    border-radius: 50%;
    cursor: pointer;
    top: 20px;
    right: 20px;
    position: fixed;
    transition: ease background-color .2s;

    &:hover {
      background-color: var(--menu-modal-close-hover-background-color);
    }
  }

  @media screen and (max-width: 1366px) {
    .modal {
      zoom: 0.8;
    }
  }

  @media screen and (max-width: 1200px) {
    .modal {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 1100px) {
    .modal {
      zoom: 0.8;
    }
  }

  @media screen and (max-height: 700px) {
    .modal {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 540px) {
    .modal {
      zoom: 0.4;
    }
  }
</style>
