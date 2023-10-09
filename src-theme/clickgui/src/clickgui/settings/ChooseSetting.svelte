<script>
    import {sineInOut} from "svelte/easing";
    import {slide} from "svelte/transition";

    export let instance;

    let name = instance.getName();
    let values = instance.getChoicesStrings();
    let value = instance.get().getChoiceName();

    let expanded = false;

    function handleToggleExpand() {
        expanded = !expanded;
    }

    function handleValueChange(v) {
        value = v;
        instance.setFromValueName(v);
    }
</script>

<div class="setting">
    <div on:click={handleToggleExpand} class:expanded={expanded} class="name">{name} - {value}</div>
    {#if expanded}
        <div class="values" transition:slide|local={{duration: 200, easing: sineInOut}}>
            {#each values as v}
                <div class="value" on:click={() => handleValueChange(v)} class:enabled={v === value}>{v}</div>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  .setting {
    padding: 7px 10px;
    overflow: hidden;
  }

  .name {
    background-color: var(--accent);
    padding: 7px 10px;
    position: relative;
    font-weight: 500;
    color: var(--text);
    font-size: 12px;
    border-radius: 5px;
    transition: ease border-radius .2s;

    &.expanded {
      border-radius: 5px 5px 0 0;

      &::after {
        transform: translateY(-50%) rotate(180deg);
      }
    }

    &::after {
      content: "";
      display: block;
      position: absolute;
      height: 10px;
      width: 10px;
      right: 10px;
      top: 50%;
      transition: ease transform .2s;
      transform: translateY(-50%);
      background-image: url("../img/settings-expand.svg");
      background-position: center;
      background-repeat: no-repeat;
    }
  }


  .values {
    background-color: rgba(0, 0, 0, 0.5);
    border-radius: 0 0 5px 5px;
    overflow: hidden;

    .value {
      color: var(--text);
      font-weight: 500;
      font-size: 12px;
      text-align: center;
      padding: 7px;
      transition: ease color .2s;

      &.enabled {
        color: var(--accent);
      }
    }
  }
</style>
