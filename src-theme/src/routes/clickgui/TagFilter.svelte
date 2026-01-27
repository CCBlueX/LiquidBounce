<script lang="ts">
    import {selectedModuleTags} from "./clickgui_store";
    import type {ModuleTagGroups} from "../../integration/types";
    import ExpandArrow from "./setting/common/ExpandArrow.svelte";
    import {slide} from "svelte/transition";
    import {convertToSpacedString, spaceSeperatedNames} from "../../theme/theme_config";

    export let groups: ModuleTagGroups = [];
    let expanded = false;
    let requiredGroups = new Set<string>();
    let tagToGroup = new Map<string, string>();
    let groupByName = new Map<string, string[]>();

    const toggleExpanded = () => {
        expanded = !expanded;
    };

    const toggleTag = (tag: string) => {
        selectedModuleTags.update((current) => {
            const set = new Set(current);
            const groupName = tagToGroup.get(tag);
            if (groupName && requiredGroups.has(groupName)) {
                const groupTags = groupByName.get(groupName) ?? [];
                const selectedCount = groupTags.filter((groupTag) => set.has(groupTag)).length;
                if (set.has(tag) && selectedCount <= 1) {
                    return Array.from(set);
                }
            }
            if (set.has(tag)) {
                set.delete(tag);
            } else {
                set.add(tag);
            }
            return Array.from(set);
        });
    };

    const toggleGroup = (groupName: string, groupTags: string[]) => {
        selectedModuleTags.update((current) => {
            const set = new Set(current);
            const allSelected = groupTags.every((tag) => set.has(tag));

            if (allSelected) {
                if (requiredGroups.has(groupName)) {
                    return Array.from(set);
                }
                groupTags.forEach((tag) => set.delete(tag));
            } else {
                groupTags.forEach((tag) => set.add(tag));
            }

            return Array.from(set);
        });
    };

    const getGroupState = (groupTags: string[]) => {
        const selected = new Set($selectedModuleTags);
        const selectedCount = groupTags.filter((tag) => selected.has(tag)).length;
        return {
            checked: selectedCount === groupTags.length,
            partial: selectedCount > 0 && selectedCount < groupTags.length
        };
    };

    $: requiredGroups = new Set(groups.filter((group) => group.required).map((group) => group.name));
    $: {
        tagToGroup = new Map();
        groupByName = new Map();
        for (const group of groups) {
            groupByName.set(group.name, group.tags);
            for (const tag of group.tags) {
                tagToGroup.set(tag, group.name);
            }
        }
    }
    $: totalTags = groups.reduce((sum, group) => sum + group.tags.length, 0);
</script>

{#if groups.length > 0}
    <div class="tag-filter" class:expanded>
        <!-- svelte-ignore a11y-no-static-element-interactions -->
        <div class="header" on:click={toggleExpanded}>
            <div class="title">Filter For Tags</div>
            <div class="count">{$selectedModuleTags.length}/{totalTags}</div>
            <div class="arrow" on:click|stopPropagation>
                <ExpandArrow bind:expanded/>
            </div>
        </div>
        {#if expanded}
            <div class="content" transition:slide|global={{duration: 200, axis: "y"}}>
                <div class="groups">
                    {#each groups as group}
                        {@const state = getGroupState(group.tags)}
                        <div class="group">
                            <!-- svelte-ignore a11y-click-events-have-key-events -->
                            <div
                                class="group-choice"
                                class:active={state.checked}
                                class:partial={state.partial}
                                on:click={() => toggleGroup(group.name, group.tags)}
                            >
                                {$spaceSeperatedNames ? convertToSpacedString(group.name) : group.name}
                            </div>
                            <div class="children">
                                {#each group.tags as tag}
                                    <!-- svelte-ignore a11y-click-events-have-key-events -->
                                    <div
                                        class="choice"
                                        class:active={$selectedModuleTags.includes(tag)}
                                        on:click={() => toggleTag(tag)}
                                    >
                                        {$spaceSeperatedNames ? convertToSpacedString(tag) : tag}
                                    </div>
                                {/each}
                            </div>
                        </div>
                    {/each}
                </div>
            </div>
        {/if}
    </div>
{/if}

<style lang="scss">
  @use "../../colors.scss" as *;

  .tag-filter {
    position: fixed;
    left: 20px;
    bottom: 20px;
    width: 230px;
    border-radius: 6px;
    box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
    color: $clickgui-text-color;
    font-size: 12px;
    z-index: 1;
    overflow: hidden;
    user-select: none;
  }

  .header {
    display: grid;
    grid-template-columns: 1fr max-content max-content;
    align-items: center;
    column-gap: 8px;
    background-color: rgba($clickgui-base-color, 0.9);
    border: none;
    border-bottom: solid 2px $accent-color;
    padding: 10px 12px;
    cursor: pointer;
  }

  .tag-filter:not(.expanded) {
    .header {
      border-bottom: none;
    }
  }

  .title {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 600;
  }

  .content {
    background-color: rgba($clickgui-base-color, 0.8);
    padding: 8px 0 10px;
  }

  .count {
    letter-spacing: 1px;
    font-weight: 500;
    font-size: 12px;
    font-family: monospace;
  }

  .groups {
    border-left: solid 2px $accent-color;
    padding: 6px 10px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .group {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .group-choice {
    color: $clickgui-text-dimmed-color;
    background-color: rgba($clickgui-base-color, 0.3);
    border-radius: 3px;
    padding: 4px 6px;
    cursor: pointer;
    font-weight: 600;
    letter-spacing: 0.4px;
    transition: ease color 0.2s;

    &:hover {
      color: $clickgui-text-color;
    }

    &.partial {
      color: $accent-color;
      box-shadow: inset 0 0 0 1px rgba($accent-color, 0.6);
    }

    &.active {
      background-color: rgba($accent-color, 0.12);
      color: $accent-color;
    }
  }

  .children {
    margin-left: 6px;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .choice {
    color: $clickgui-text-dimmed-color;
    background-color: rgba($clickgui-base-color, 0.3);
    border-radius: 3px;
    padding: 3px 6px;
    cursor: pointer;
    font-weight: 500;
    transition: ease color 0.2s;
    overflow-wrap: anywhere;

    &:hover {
      color: $clickgui-text-color;
    }

    &.active {
      background-color: rgba($accent-color, 0.1);
      color: $accent-color;
    }
  }

  .arrow :global(button.arrow) {
    cursor: pointer;
  }
</style>
