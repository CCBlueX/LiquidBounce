<script lang="ts">
    import {onMount, tick} from "svelte";
    import type {Module} from "../../../integration/types";
    import {getModules} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import {getTextWidth} from "../../../integration/text_measurement";
    import {flip} from "svelte/animate";
    import {fly} from "svelte/transition";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    let enabledModules: Module[] = [];

    async function updateEnabledModules() {
        const modules = await getModules();
        const visibleModules = modules.filter(m => m.enabled && !m.hidden);

        const modulesWithWidths = visibleModules.map(module => {
                let formattedName = $spaceSeperatedNames ? convertToSpacedString(module.name) : module.name;
                let fullName = module.tag == null ? formattedName : formattedName + " " + module.tag;

                return {
                    ...module,
                    width: getTextWidth(fullName, "500 14px Inter")
                };
            }
        );

        modulesWithWidths.sort((a, b) => b.width - a.width);

        enabledModules = modulesWithWidths;
        await tick();
    }

    spaceSeperatedNames.subscribe(async () => {
        await updateEnabledModules();
    });

    onMount(async () => {
        await updateEnabledModules();
    });

    listen("moduleToggle", async () => {
        await updateEnabledModules();
    });

    listen("refreshArrayList", async () => {
        await updateEnabledModules();
    });
</script>

<div class="arraylist">
    {#each enabledModules as {name, tag} (name)}
        <div class="module" animate:flip={{ duration: 200 }} transition:fly={{ x: 50, duration: 200 }}>
            {$spaceSeperatedNames ? convertToSpacedString(name) : name}
            {#if tag}
                <span class="tag"> {tag}</span>
            {/if}
        </div>
    {/each}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .module {
    background-color: rgba($arraylist-base-color, 0.68);
    color: $arraylist-text-color;
    font-size: 14px;
    border-radius: 4px 0 0 4px;
    padding: 5px 8px;
    border-left: solid 4px $accent-color;
    width: max-content;
    font-weight: 500;
    margin-left: auto;
  }

  .tag {
    color: $arraylist-tag-color;
  }
</style>
