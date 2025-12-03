<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {fly} from "svelte/transition";

    export let name: string | null;
    export let options: string[];
    export let value: string;

    const dispatch = createEventDispatcher();

    $: currentIndex = options.indexOf(value);

    let transitionDirection = 1;

    function cycle(direction: number) {
        transitionDirection = direction;
        let newIndex = currentIndex + direction;

        if (newIndex < 0) {
            newIndex = options.length - 1;
        } else if (newIndex >= options.length) {
            newIndex = 0;
        }

        value = options[newIndex];
        dispatch("change");
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="cycle-button">
    <div class="button-left" on:click={() => cycle(-1)}>
        <div class="icon left-arrow"></div>
    </div>

    <div class="content">
        {#if name !== null}
            <span class="name">{$spaceSeperatedNames ? convertToSpacedString(name) : name}</span>
        {/if}
        <span class="value">
            {#key value}
                <span
                        class="value-item"
                        in:fly={{ x: -20 * transitionDirection, duration: 200 }}
                        out:fly={{ x: 20 * transitionDirection, duration: 200 }}
                >{$spaceSeperatedNames ? convertToSpacedString(value) : value}</span>
            {/key}
        </span>
    </div>

    <div class="button-right" on:click={() => cycle(1)}>
        <div class="icon right-arrow"></div>
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .cycle-button {
    display: flex;
    align-items: center;
    background-color: $accent-color;
    border-radius: 3px;
    overflow: hidden;
    height: 28px;
  }

  .button-left, .button-right {
    flex: 0 0 auto;
    width: 30px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    user-select: none;
    transition: background-color 0.2s;

    &:hover {
      background-color: rgba(255, 255, 255, 0.1);
    }
  }

  .content {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 10px;
    min-width: 0;

    .name {
      font-weight: 500;
      color: $clickgui-text-color;
      font-size: 12px;
      margin-right: 8px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .value {
      display: grid;

      .value-item {
        grid-area: 1 / 1;
        font-weight: 500;
        color: $clickgui-text-color;
        font-size: 12px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .icon {
    height: 10px;
    width: 10px;
    background-image: url("/img/clickgui/icon-settings-expand.svg");
    background-position: center;
    background-repeat: no-repeat;
    transform-origin: 50% 50%;
  }

  .left-arrow {
    transform: rotate(90deg);
  }

  .right-arrow {
    transform: rotate(-90deg);
  }
</style>
