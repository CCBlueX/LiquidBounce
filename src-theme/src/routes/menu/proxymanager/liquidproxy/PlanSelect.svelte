<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {slide} from "svelte/transition";
    import {quintOut} from "svelte/easing";
    import GenericSelect from "../../common/setting/select/GenericSelect.svelte";
    import type {LiquidProxyPlan} from "../../../../integration/types";

    export let plans: LiquidProxyPlan[];
    export let level: number;

    const SMART_LEVEL = 0;

    const dispatch = createEventDispatcher<{ change: number }>();

    $: selected = plans.find(plan => plan.level === level);
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<GenericSelect closeOnInternalClick={true}>
    <span slot="title"><span class="title">Type</span> {selected?.name ?? "Smart"}</span>

    <svelte:fragment slot="options">
        {#each plans as plan (plan.level)}
            <div class="option" class:active={plan.level === level}
                 on:click={() => dispatch("change", plan.level)}
                 transition:slide|global={{ duration: 200, easing: quintOut }}>
                {plan.name}
                {#if plan.level === SMART_LEVEL}
                    <span class="recommended">Recommended</span>
                {/if}
            </div>
        {/each}
    </svelte:fragment>
</GenericSelect>

<style lang="scss">
  .title {
    font-weight: 600;
  }

  .option {
    display: flex;
    align-items: center;
    column-gap: 10px;
    font-weight: 500;
    color: var(--menu-text-color);
    font-size: 20px;
    padding: 15px 20px;
    transition: ease color .2s;

    &:hover {
      color: var(--accent-color);
    }

    &.active {
      color: var(--accent-color);
    }
  }

  .recommended {
    background-color: var(--success-color);
    color: var(--menu-text-color);
    font-size: 11px;
    font-weight: 500;
    padding: 3px 8px;
    border-radius: 20px;
  }
</style>
