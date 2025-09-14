<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";

    export let title: string;
    export let icon: string;
    export let disabled = false;


    const MIN_HOVER_TIME = 50;
    let buttonWidth = 60;
    let titleElement: HTMLDivElement;

    const dispatch = createEventDispatcher();

    let hoverTimer: number;
    let isIntent = false;

    onMount(() => {
        if (titleElement) {
            const titleWidth = titleElement.offsetWidth;
            buttonWidth = 60 + titleWidth + 12;
        }
    });

    function handleMouseEnter() {

        clearTimeout(hoverTimer);
        isIntent = false;
        hoverTimer = window.setTimeout(() => {
            isIntent = true;
        }, MIN_HOVER_TIME);
    }

    function handleMouseLeave() {
        clearTimeout(hoverTimer);
        if (isIntent) {
            isIntent = false;
        }
    }
</script>

<button
        class="icon-text-button"
        class:intent={isIntent}
        {disabled}
        on:click={() => dispatch("click")}
        on:mouseenter={handleMouseEnter}
        on:mouseleave={handleMouseLeave}
        style="--button-width: {buttonWidth}px"
>
    <div class="icon-container">
        <div class="icon">
            <img alt={title} draggable="false" src="img/menu/{icon}"/>
        </div>
        <div bind:this={titleElement} class="title">{title}</div>
    </div>
</button>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .icon-text-button {
    --button-bg: rgba(0, 0, 0, 0.2);
    --button-border: rgba(255, 255, 255, 0.06);
    --button-text: #{$text};
    --button-icon-bg: rgba(255, 255, 255, 0.05);
    --button-shadow: 0 6px 12px rgba(0, 0, 0, 0.2);
    --button-highlight: 0 0 6px rgba(255, 255, 255, 0.05);
    --button-text-shadow: 0 0 3px rgba(0, 0, 0, 0.2);

    display: inline-flex;
    padding: 0;
    border-radius: 30px;
    background: var(--button-bg);
    border: 1px solid var(--button-border);
    backdrop-filter: blur(10px);
    box-shadow: 0 6px 8px rgba(0, 0, 0, 0.24);
    font-size: 24px;
    color: var(--button-text);
    transition: width 0.3s ease, box-shadow 0.3s ease;
    overflow: hidden;
    height: 60px;
    width: 60px;
    margin: 4px 8px;

    &[disabled] {
      opacity: 0.4;
      pointer-events: none;
    }

    &.intent {

      width: var(--button-width);
      box-shadow: 0 10px 10px rgba(0, 0, 0, 0.24);

      .title {
        opacity: 1;
      }

      .icon img {
        opacity: 1;
      }
    }

    .icon-container {
      display: flex;
      align-items: center;
      height: 100%;
      box-shadow: inset 0 4px 8px rgba(255, 255, 255, 0.03);
    }

    .icon {
      width: 60px;
      height: 60px;
      min-width: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--button-icon-bg);
      border-radius: 30px;
      transition: all 0.5s ease;

      img {
        width: 32px;
        height: 32px;
        opacity: 0.75;
        object-fit: contain;
        display: block;
        transition: opacity 0.2s ease;
      }
    }

    .title {
      white-space: nowrap;
      text-shadow: var(--button-text-shadow);
      padding: 0 12px 0 10px;
      opacity: 0;
      transition: opacity 0.3s ease 0.1s;
    }
  }
</style>
