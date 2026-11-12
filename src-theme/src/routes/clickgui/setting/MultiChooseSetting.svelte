<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, MultiChooseSetting,} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {SortableList} from "@jhubbardsf/svelte-sortablejs";
    import SettingRow from "./common/SettingRow.svelte";
    import DraggableItem from "./common/DraggableItem.svelte";
    import RemovableItem from "./common/RemovableItem.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as MultiChooseSetting;

    let errorValue: string | null = null;
    let timeoutId: ReturnType<typeof setTimeout>;
    let sortableRenderKey = 0;

    const dispatch = createEventDispatcher();

    $: label = (choice: string) => $spaceSeperatedNames ? convertToSpacedString(choice) : choice;
    $: unchosen = cSetting.choices.filter(choice => !cSetting.value.includes(choice));

    function commit() {
        setting = {...cSetting};
        dispatch("change");
    }

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

        commit();
    }

    type SortEvent = {
        oldIndex?: number | null;
        newIndex?: number | null;
        oldDraggableIndex?: number | null;
        newDraggableIndex?: number | null;
    };

    function handleSort(e: SortEvent) {
        const oldIndex = e.oldDraggableIndex ?? e.oldIndex;
        const newIndex = e.newDraggableIndex ?? e.newIndex;
        if (oldIndex == null || newIndex == null || oldIndex === newIndex) {
            return;
        }

        const values = [...cSetting.value];
        const [moved] = values.splice(oldIndex, 1);
        if (moved === undefined) {
            return;
        }
        values.splice(newIndex, 0, moved);

        cSetting.value = values;
        commit();
        // Sortable moved the elements itself, so the list is built again from the value
        sortableRenderKey++;
    }
</script>

<SettingRow {setting}>
    <span class="count">{cSetting.value.length} of {cSetting.choices.length}</span>

    <svelte:fragment slot="below">
        {#if cSetting.isOrderSensitive}
            {#if cSetting.value.length > 0}
                <div class="ordered">
                    {#key sortableRenderKey}
                        <SortableList class="" forceFallback={true} fallbackOnBody={true} animation={150}
                                      onEnd={handleSort}>
                            {#each cSetting.value as choice, index}
                                <DraggableItem>
                                    <RemovableItem on:remove={() => handleChange(choice)}>
                                        <span class="position">{index + 1}</span>
                                        <span class="chosen" class:error={errorValue === choice}>{label(choice)}</span>
                                    </RemovableItem>
                                </DraggableItem>
                            {/each}
                        </SortableList>
                    {/key}
                </div>
            {/if}
            {#if unchosen.length > 0}
                <div class="chips">
                    {#each unchosen as choice (choice)}
                        <button type="button" class="chip add" on:click={() => handleChange(choice)}>
                            + {label(choice)}
                        </button>
                    {/each}
                </div>
            {/if}
        {:else}
            <div class="chips">
                {#each cSetting.choices as choice (choice)}
                    <button
                            type="button"
                            class="chip"
                            class:active={cSetting.value.includes(choice)}
                            class:error={errorValue === choice}
                            aria-pressed={cSetting.value.includes(choice)}
                            on:click={() => handleChange(choice)}
                    >
                        {label(choice)}
                    </button>
                {/each}
            </div>
        {/if}
    </svelte:fragment>
</SettingRow>

<style lang="scss">
  .count {
    font-size: 11px;
    color: var(--clickgui-text-muted-color);
    white-space: nowrap;
  }

  .chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding-bottom: 8px;
  }

  .chip {
    max-width: 100%;
    padding: 3px 9px;
    border: 1px solid var(--clickgui-chip-border-color);
    border-radius: 999px;
    background-color: var(--clickgui-selection-chip-background-color);
    color: var(--clickgui-selection-chip-color);
    font-family: "Inter", sans-serif;
    font-size: 11px;
    font-weight: 500;
    overflow-wrap: anywhere;
    cursor: pointer;
    transition: ease color .15s, ease background-color .15s, ease border-color .15s;

    &:hover, &:focus-visible {
      color: var(--clickgui-text-color);
      border-color: var(--clickgui-control-border-color);
    }

    &.active {
      background-color: var(--clickgui-selection-chip-selected-background-color);
      color: var(--clickgui-selection-chip-selected-color);
      border-color: var(--clickgui-chip-active-border-color);
    }

    &.add {
      padding-left: 7px;
      border-style: dashed;
      border-color: var(--clickgui-control-border-color);
      background: none;
    }

    &.error {
      background-color: var(--clickgui-selection-chip-remove-background-color);
      color: var(--clickgui-selection-chip-remove-color);
      border-color: var(--clickgui-selection-chip-remove-color);
    }
  }

  .ordered {
    padding-bottom: 3px;
  }

  .position {
    flex: none;
    width: 16px;
    font-size: 11px;
    font-weight: 600;
    color: var(--accent-color);
    text-align: center;
  }

  .chosen {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    transition: ease color .15s;

    &.error {
      color: var(--clickgui-selection-chip-remove-color);
    }
  }
</style>
