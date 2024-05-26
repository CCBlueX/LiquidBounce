<script lang="ts">
  import { onMount } from "svelte";
  import { getModules } from "../../../integration/rest";
  import type { GroupedModules, Module } from "../../../integration/types";
  import { groupByCategory } from "../../../integration/util";
  import Panel from "./Panel.svelte";
  import Search from "../Search.svelte";
  let modules: Module[] = [];
  let categories: GroupedModules = {};

  export let scaleFactor;

  onMount(async () => {
    modules = await getModules();
    categories = groupByCategory(modules);
  });
</script>

<Search modules={structuredClone(modules)} />

{#each Object.entries(categories) as [category, modules], panelIndex}
  <Panel {category} {modules} {panelIndex} {scaleFactor} />
{/each}
