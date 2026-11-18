<script lang="ts">
    import {fade, fly} from "svelte/transition";
    import {createEventDispatcher} from "svelte";
    import {portal} from "../../../../integration/util";
    import {escapeLayer} from "../../../../integration/escape";

    export let title: string;
    export let visible: boolean;

    const dispatch = createEventDispatcher();

    function handleClick() {
        dispatch("close");
        visible = false;
    }

    const layer = escapeLayer(handleClick);
    $: layer.setOpen(visible);
</script>

{#if visible}
    <div class="modal-wrapper" transition:fade|global={{duration: 140}} use:portal>
        <div class="modal" in:fly|global={{duration: 180, y: -20}} out:fade|global={{duration: 120}}>
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
    backdrop-filter: blur(5px);
    z-index: 999;
  }

  .modal {
    background-color: var(--menu-modal-background-color);
    backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturation));
    min-width: 500px;
    max-width: calc(100vw - 64px);
    max-height: calc(100vh - 64px);
    overflow-y: auto;
    position: fixed;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    padding: 36px;
    display: flex;
    flex-direction: column;
    border: 1px solid var(--accent-border-color);
    border-radius: var(--radius-lg);
    box-shadow: var(--elevation-lg) var(--menu-modal-shadow-color);
  }

  .title {
    color: var(--menu-text-color);
    font-size: 32px;
    font-weight: 600;
    letter-spacing: -0.025em;
    position: relative;
    width: max-content;
    max-width: calc(100% - 40px);
    align-self: center;
    margin-bottom: 64px;

    &::after {
      content: "";
      position: absolute;
      display: block;
      height: 3px;
      width: 64%;
      background-color: var(--menu-modal-title-accent-color);
      bottom: -18px;
      left: 50%;
      transform: translateX(-50%);
      border-radius: var(--radius-pill);
    }
  }

  .content {
    display: flex;
    flex-direction: column;
    row-gap: 32px;
  }

  .button-modal-close {
    height: 36px;
    width: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: transparent;
    border: solid 1px var(--accent-border-color);
    border-radius: 50%;
    cursor: pointer;
    top: 18px;
    right: 18px;
    position: absolute;
    transition: background-color var(--duration-fast) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard);

    &:hover {
      background-color: var(--menu-modal-close-hover-background-color);
      border-color: var(--accent-border-strong-color);
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
