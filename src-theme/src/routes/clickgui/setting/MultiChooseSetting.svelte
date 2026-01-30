<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, MultiChooseSetting,} from "../../../integration/types";
    import {slide} from "svelte/transition";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import {setItem} from "../../../integration/persistent_storage";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as MultiChooseSetting;
    const thisPath = `${path}.${cSetting.name}`;

    let errorValue: string | null = null;
    let timeoutId: ReturnType<typeof setTimeout>;

    const dispatch = createEventDispatcher();

    function handleChange(v: string) {
        if (cSetting.value.includes(v)) {
            const filtered = cSetting.value.filter(item => item !== v);

            if (filtered.length === 0 && !cSetting.canBeNone) {
                // Doesn't remove the element because in this case the value will be empty
                // And indicate the value
                errorValue = v
                clearTimeout(timeoutId);
                timeoutId = setTimeout(() => errorValue = null, 300);

                return;
            }

            cSetting.value = filtered;
        } else {
            cSetting.value = [...cSetting.value, v]
        }

        setting = {...cSetting};
        dispatch("change");
    }

    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

    function toggleExpanded() {
        expanded = !expanded;
    }
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
            {#each cSetting.choices as choice (choice)}
                <div
                        class="choice"
                        class:active={cSetting.value.includes(choice)}
                        class:error={errorValue === choice}
                        on:click={() => {
                            handleChange(choice)
                        }}
                >
                    {$spaceSeperatedNames ? convertToSpacedString(choice) : choice}
                </div>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../colors" as *;

  .setting {
    padding: 7px 0;
    color: var(--clickgui-base-text-color);
  }

  .title {
    color: var(--clickgui-base-text-color);
    font-size: 12px;
    font-weight: 600;
  }

  .choice {
    color: var(--clickgui-base-text-dimmed-color);
    background-color: var(--clickgui-background-color-light);
    border-radius: 3px;
    padding: 3px 6px;
    cursor: pointer;
    font-weight: 500;
    transition: ease color 0.2s;
    overflow-wrap: anywhere;

    &:hover {
      color: var(--clickgui-base-text-color);
    }

    &.error {
      background-color: var(--clickgui-multi-choose-setting-error-color) !important;
      color: var(--clickgui-multi-choose-setting-error-color) !important;
    }

    &.active {
      background-color: var(--clickgui-base-accent-dimmed-color);
      color: var(--clickgui-base-accent-color);
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
    border-left: solid 2px var(--clickgui-base-accent-color);
    color: var(--clickgui-base-text-color);
    padding: 7px 7px;
    display: flex;
    flex-wrap: wrap;
    gap: 7px;
    font-size: 12px;
  }
</style>
