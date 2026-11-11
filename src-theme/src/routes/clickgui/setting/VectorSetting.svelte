<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {BlockHitResult, ModuleSetting, Setting, Vec, Vec3Setting, VecAxis} from "../../../integration/types";
    import SettingRow from "./common/SettingRow.svelte";
    import {getCrosshairData, getPlayerData} from "../../../integration/rest";

    export let setting: ModuleSetting;
    export let vecAxes: VecAxis[];
    export let step: number;

    const cSetting = setting as Setting<Vec<typeof vecAxes[number]>>;
    const useLocateButton = (setting as Vec3Setting).useLocateButton ?? false;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    async function locate() {
        const hitResult = await getCrosshairData();

        if (hitResult.type === "block") {
            const blockHitResult = hitResult as BlockHitResult;
            (cSetting as Vec3Setting).value = blockHitResult.blockPos;
        } else {
            const playerData = await getPlayerData();
            (cSetting as Vec3Setting).value = playerData.blockPosition;
        }
        handleChange();
    }
</script>

<SettingRow {setting}>
    <div class="vector">
        {#each vecAxes as axis (axis)}
            <label class="axis">
                <span class="axis-name">{axis.toUpperCase()}</span>
                <input
                        type="number"
                        {step}
                        class="value"
                        spellcheck="false"
                        bind:value={cSetting.value[axis]}
                        on:input={handleChange}
                />
            </label>
        {/each}
        {#if useLocateButton}
            <button type="button" class="locate" on:click={locate} title="Locate" aria-label="Locate">&#x2299;</button>
        {/if}
    </div>
</SettingRow>

<style lang="scss">
  @use "./common/underlined-input" as *;

  .vector {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    align-items: center;
    gap: 3px 8px;
    min-width: 0;
  }

  .axis {
    display: flex;
    align-items: center;
    gap: 5px;
  }

  .axis-name {
    font-size: 10px;
    font-weight: 600;
    color: var(--clickgui-text-muted-color);
  }

  .value {
    @include underlined-input(26px);
    width: 58px;
    padding: 0 6px;
    appearance: textfield;

    &::-webkit-outer-spin-button,
    &::-webkit-inner-spin-button {
      -webkit-appearance: none;
      margin: 0;
    }
  }

  .locate {
    width: 20px;
    height: 20px;
    border: none;
    border-radius: 3px;
    background: none;
    color: var(--clickgui-text-color);
    font-size: 13px;
    line-height: 1;
    opacity: .7;
    cursor: pointer;
    transition: ease opacity .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-control-hover-background-color);
    }
  }
</style>
