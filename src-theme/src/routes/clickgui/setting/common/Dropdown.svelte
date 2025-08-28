<script lang="ts">
    import {createEventDispatcher, afterUpdate} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {fade} from 'svelte/transition';
    import { FadeIn,FadeOut } from "../../../../util/animate_utils";
    import {cubicOut} from "svelte/easing";

    export let name: string | null;
    export let options: string[];
    export let value: string;

    const dispatch = createEventDispatcher();

    let expanded = false;
    let optionRefs: HTMLElement[] = [];

    function updateValue(v: string) {
        value = v;
        dispatch("change");
    }
    
    function easeInBack(t: number): number {
        const c1 = 1.5;
        const c3 = c1 + 1;
        return c3 * t * t * t - c1 * t * t;
    }

    afterUpdate(() => {
        if (expanded && optionRefs.length > 0) {

        }
    });


</script>


<!-- svelte-ignore a11y_no_static_element_interactions -->
<!-- svelte-ignore a11y_click_events_have_key_events -->
<div class="dropdown" class:expanded>
    <div class="head" on:click={() => (expanded = !expanded)}>
        {#if name !== null}
      <span class="text">
        {$spaceSeperatedNames ? convertToSpacedString(name) : name}
          &bull; {$spaceSeperatedNames ? convertToSpacedString(value) : value}
      </span>
        {:else}
            <span class="text">{$spaceSeperatedNames ? convertToSpacedString(value) : value}</span>
        {/if}
    </div>

    {#if expanded}
        <div
                class="overlay"
                transition:fade={{ duration: 100, easing: cubicOut }}
                on:contextmenu={() => (expanded = !expanded)}
        >
            <div
                    class="options"
                    on:click|stopPropagation
                    in:FadeIn={{ duration: 200 }}
                    out:FadeOut={{ duration: 200 }}
                    style="max-height: min(66vh, calc(0.66 * var(--panel-height, 100vh)))"

            >
                {#each options as o, index (o)}
                    <div
                            class="option"
                            on:contextmenu|stopPropagation
                            class:active={o === value}
                            on:click={() => updateValue(o)}
                            bind:this={optionRefs[index]}
                    >
          <span class="option-content">
            {$spaceSeperatedNames ? convertToSpacedString(o) : o}
          </span>
                    </div>
                {/each}
            </div>
        </div>
    {/if}
</div>
<style lang="scss">
  @use "../../../../colors.scss" as *;
  @use "sass:color";

  .dropdown {
    position: relative;

    &.expanded {
      .head {
        border-color: color-mix(in srgb, var(--primary-color) 40%, transparent);
        box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color) 20%, transparent), 0 8px 32px rgba(0, 0, 0, 0.3);

      }
    }
  }

  .head {
    background: rgba($base, 0.15);
    border: 2px solid rgba($text, 0.1);
    padding: 8px 12px;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    position: relative;
    border-radius: 6px;
    transition: border-radius 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    border-color 0.3s ease,
    box-shadow 0.3s ease,
    background 0.3s ease;
    overflow: hidden;

    &:hover {
      border-color: rgba($text, 0.2);
      box-shadow: 0 4px 12px rgba($text, 0.15);
      background: rgba($base, 0.25);
    }

    .text {
      font-weight: 500;
      color: $text;
      font-size: var(--font-size);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-right: 12px;
      position: relative;
      z-index: 1;
    }

    .text::after {
      content: "";
      display: block;
      position: absolute;
      height: 12px;
      width: 12px;
      right: 8px;
      top: 50%;
      background-position: center;
      background-repeat: no-repeat;
      transform-origin: 50% 50%;
      transition: opacity 0.25s ease,
      transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
      filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.2));
      z-index: 1;
    }
  }


  .overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(4px);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 9999;
    transition: z-index 0s linear 300ms;
    pointer-events: auto;

    * {
      pointer-events: auto;
    }

    .options {
      position: relative;

      background: linear-gradient(
                      145deg,
                      rgba($base, 0.82) 0%,
                      rgba(color.adjust($base, $lightness: -8%), 0.78) 100%
      );

      mask-size: 100% 100%;
      mask-repeat: no-repeat;
      border-radius: 14px;
      padding: 8px;
      width: min(90%, 420px);
      backdrop-filter: blur(24px);
      -webkit-backdrop-filter: blur(24px);
      overflow-y: auto;
      overflow-x: hidden;
      box-shadow: 0 16px 64px rgba(0, 0, 0, 0.32),
      0 0 0 1px rgba(255, 255, 255, 0.08) inset,
      0 0 0 2px rgba(0, 0, 0, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.12);

      &::-webkit-scrollbar {
        width: 8px;
        height: 8px;
      }

      &::-webkit-scrollbar-thumb {
        background: linear-gradient(
                        to bottom,
                        rgba($text, 0.15) 0%,
                        rgba($text, 0.2) 50%,
                        rgba($text, 0.15) 100%
        );
        border-radius: 4px;
        border: 1px solid rgba(255, 255, 255, 0.1);
        -webkit-backface-visibility: hidden;
        transform: translate3d(0, 0, 0);
        contain: content;
        transition: background 0.3s ease-out, border-color 0.2s linear;
        backdrop-filter: none;
      }

      &::-webkit-scrollbar-track {
        background: rgba($base, 0.1);
        border-radius: 0 4px 4px 0;
        transform: translateZ(0);
      }

      &::-webkit-scrollbar-thumb:hover {
        background: linear-gradient(
                        180deg,
                        rgba($text, 0.3) 0%,
                        rgba($text, 0.45) 50%,
                        rgba($text, 0.3) 100%
        );
        border: 1px solid rgba(255, 255, 255, 0.2);
        box-shadow: inset 0 0 4px rgba(255, 255, 255, 0.1),
        0 0 6px rgba($text, 0.3);
        cursor: pointer;
      }

      &::-webkit-scrollbar-corner {
        background: transparent;
      }

      &::-webkit-scrollbar-button {
        background-color: rgba(255, 255, 255, 0.2);
        width: 16px;
        height: 16px;
        display: inline-block;
      }

      &::-webkit-scrollbar-button:vertical:decrement,
      &::-webkit-scrollbar-button:vertical:increment {
        background: transparent no-repeat center;
      }

      .option {
        padding: 14px 8px;
        margin: 2px 4px;
        border-radius: 8px;
        color: rgba($text, 0.6);
        font-size: calc(var(--font-size) + 1px);
        font-weight: 500;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.28s cubic-bezier(0.33, 1, 0.68, 1);
        position: relative;
        overflow: hidden;
        z-index: 1;


        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          height: 1px;
          background: linear-gradient(
                          90deg,
                          transparent 0%,
                          rgba(255, 255, 255, 0.03) 20%,
                          rgba(255, 255, 255, 0.05) 50%,
                          rgba(255, 255, 255, 0.03) 80%,
                          transparent 100%
          );
        }

        &:hover {
          color: rgba($text, 0.8);
          box-shadow: 0 2px 8px color-mix(in srgb, var(--primary-color) 10%, transparent),
          0 0 0 1px color-mix(in srgb, var(--secondary-color) 15%, transparent);
          background: radial-gradient(
                          circle at center,
                          color-mix(in srgb, var(--primary-color) 30%, transparent) 0%,
                          transparent 80%
          );

          &::after {
            opacity: 0.06;
          }
        }

        &.active {
          color: $text;
        }

        &.active:hover {
          box-shadow: 0 2px 8px color-mix(in srgb, var(--primary-color) 10%, transparent),
          0 0 0 1px color-mix(in srgb, var(--secondary-color) 15%, transparent);
        }

        &::after {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          opacity: 0.4;
          transition: opacity 0.3s ease, background 0.3s ease;
          pointer-events: none;
        }

        .option-content {
          position: relative;
          z-index: 1;
          letter-spacing: 0.02em;
          text-align: center;
          width: 100%;
        }
      }
    }
  }

</style>
