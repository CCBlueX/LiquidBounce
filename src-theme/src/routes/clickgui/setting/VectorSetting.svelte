<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {BlockHitResult, ModuleSetting, Setting, Vec, Vec3Setting, VecAxis} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
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

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <div class="input-group"
         style="grid-template-columns: repeat({vecAxes.length}, 1fr) {useLocateButton ? '20px' : ''}">
        {#each vecAxes as axis (axis)}
            <input
                    type="number"
                    {step}
                    class="value"
                    spellcheck="false"
                    placeholder={axis.toUpperCase()}
                    bind:value={cSetting.value[axis]}
                    on:input={handleChange}
            />
        {/each}
        {#if useLocateButton}
            <button class="locate-btn" on:click={locate} title="Locate">&#x2299;</button>
        {/if}
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
  }

  .name {
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    margin-bottom: 5px;
  }

  .input-group {
    display: grid;
    column-gap: 5px;

    input.value {
      width: 100%;
      background-color: rgba($clickgui-base-color, .36);
      font-family: monospace;
      font-size: 12px;
      color: $clickgui-text-color;
      border: none;
      border-bottom: solid 2px $accent-color;
      padding: 5px;
      border-radius: 3px;
      transition: ease border-color .2s;
      appearance: textfield;

      &::-webkit-scrollbar {
        background-color: transparent;
      }

      /* Hide the number input spinner buttons */
      &::-webkit-outer-spin-button,
      &::-webkit-inner-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }

    .locate-btn {
      display: block;
      background-color: transparent;
      border: none;
      cursor: pointer;
      color: $clickgui-text-color;
      font-size: 12px;
      text-align: right;
    }
  }
</style>
