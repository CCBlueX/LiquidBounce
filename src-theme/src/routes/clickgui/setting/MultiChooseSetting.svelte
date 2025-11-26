<script lang="ts">
    import {createEventDispatcher, onDestroy} from "svelte";
    import type {ModuleSetting, MultiChooseSetting,} from "../../../integration/types";
    import {slide} from "svelte/transition";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import {setItem} from "../../../integration/persistent_storage";
    import {flip} from "svelte/animate";
    import {swap} from "../../../integration/util";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as MultiChooseSetting;
    const thisPath = `${path}.${cSetting.name}`;

    let errorValue: string | null = null;
    let timeoutId: ReturnType<typeof setTimeout>;

    const dispatch = createEventDispatcher();

    const isEnabled = (choice: string) => cSetting.value.includes(choice);

    const getEnabledIndex = (choice: string) => {
        if (!cSetting.isOrderSensitive || !isEnabled(choice)) return -1;
        return cSetting.value.indexOf(choice);
    }

    $: choices = (() => {
        if (cSetting.isOrderSensitive) {
            return [...cSetting.value, ...cSetting.choices.filter(it => !isEnabled(it))];
        } else {
            return cSetting.choices;
        }
    })();

    let hoveringChoice = -1;

    const moveEnabledChoice = (index: number, direction: number) => {
        if (!cSetting.isOrderSensitive) return;

        const enabledCount = cSetting.value.length;
        if (index < 0 || index >= enabledCount) return;

        const newIndex = index + direction;
        if (newIndex < 0 || newIndex >= enabledCount) return;

        swap(cSetting.value, index, newIndex);
        cSetting.value = cSetting.value;
        fireChange();
    }

    const fireChange = () => {
        setting = {...cSetting};
        dispatch("change");
    };

    const handleChange = (v: string) => {
        if (isEnabled(v)) {
            const filtered = cSetting.value.filter(item => item !== v);

            if (filtered.length === 0 && !cSetting.canBeNone) {
                // Doesn't remove the element because in this case the value will be empty
                // And indicate the value
                errorValue = v;
                clearTimeout(timeoutId);
                timeoutId = setTimeout(() => errorValue = null, 300);

                return;
            }

            cSetting.value = filtered;
        } else {
            // Append enabled value to tail
            cSetting.value = [...cSetting.value, v];
        }

        fireChange();
    };

    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

    function toggleExpanded() {
        expanded = !expanded;
    }

    onDestroy(() => clearTimeout(timeoutId));
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="setting">
    <div class="head" class:expanded on:contextmenu|preventDefault={toggleExpanded}>
        <div class="title">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <div class="amount">{cSetting.value.length}/{cSetting.choices.length}</div>
        <ExpandArrow bind:expanded/>
    </div>

    {#if expanded}
        <div class="choices" transition:slide|global={{duration: 200, axis: "y"}}>
            {#each choices as choice, i (choice)}
                {@const enabledIndex = getEnabledIndex(choice)}
                {@const showLeftArrow = cSetting.isOrderSensitive && enabledIndex > 0 && hoveringChoice === i}
                {@const showRightArrow = cSetting.isOrderSensitive && enabledIndex >= 0 && enabledIndex < cSetting.value.length - 1 && hoveringChoice === i}
                <div
                        class="choice"
                        class:active={isEnabled(choice)}
                        class:error={errorValue === choice}
                        class:order-sensitive={cSetting.isOrderSensitive}
                        animate:flip={{duration: 200}}
                        on:mouseenter={() => hoveringChoice = i}
                        on:mouseleave={() => hoveringChoice = -1}
                >
                    <!-- Move left -->
                    <span 
                        class="choice-action move-left" 
                        class:visible={showLeftArrow}
                        on:click|stopPropagation={() => moveEnabledChoice(enabledIndex, -1)} 
                    >
                        ◀
                    </span>

                    <!-- Toggle -->
                    <span class="choice-name" on:click={() => handleChange(choice)}>
                        {$spaceSeperatedNames ? convertToSpacedString(choice) : choice}
                    </span>

                    <!-- Move right -->
                    <span 
                        class="choice-action move-right" 
                        class:visible={showRightArrow}
                        on:click|stopPropagation={() => moveEnabledChoice(enabledIndex, 1)} 
                    >
                        ▶
                    </span>
                </div>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
    color: $clickgui-text-color;
  }

  .title {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 600;
  }

  .choice {
    color: $clickgui-text-dimmed-color;
    background-color: rgba($clickgui-base-color, 0.3);
    border-radius: 3px;
    padding: 3px 6px;
    cursor: pointer;
    font-weight: 500;
    transition: ease color 0.2s, ease background-color 0.2s;
    overflow-wrap: anywhere;
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 3px;

    &:hover {
      color: $clickgui-text-color;
    }

    &.error {
      background-color: rgba($menu-error-color, 0.1) !important;
      color: $menu-error-color !important;
    }

    &.active {
      background-color: rgba($accent-color, 0.1);
      .choice-name {
        color: $accent-color;
      }
    }

    &:not(.order-sensitive) {
      gap: 0;

      .choice-action {
        display: none;
      }
    }

    .choice-action {
      color: $clickgui-text-dimmed-color;
      font-size: 10px;
      cursor: pointer;
      padding: 1px 2px;
      border-radius: 2px;
      transition: ease color 0.2s, ease background-color 0.2s, ease width 0.2s, ease opacity 0.2s;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 0;
      height: 14px;
      opacity: 0;
      overflow: hidden;

      &.visible {
        width: 14px;
        opacity: 1;
      }

      &:hover {
        color: $accent-color;
        background-color: rgba($accent-color, 0.1);
      }
    }
  }

  .amount {
    letter-spacing: 1px;
    font-weight: 500;
    font-size: 12px;
    font-family: monospace;
  }

  .head {
    display: grid;
    grid-template-columns: 1fr max-content max-content;
    transition: ease margin-bottom .2s;
    align-items: center;

    &.expanded {
      margin-bottom: 10px;
    }
  }

  .choices {
    border-left: solid 2px $accent-color;
    color: $clickgui-text-color;
    padding: 7px 7px;
    display: flex;
    flex-wrap: wrap;
    gap: 7px;
    font-size: 12px;
  }
</style>
