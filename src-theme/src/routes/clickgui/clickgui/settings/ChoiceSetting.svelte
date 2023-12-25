<script>
    import {sineInOut} from "svelte/easing";
    import {fade, slide} from "svelte/transition";
    import GenericSetting from "./GenericSetting.svelte";

    /**
     * A reference to the value instance of this setting. It is part of the module configurable and should NOT lose its reference.
     */
    export let reference;
    /**
     * This function is passed from the parent component and is used to write the new configurable to the client.
     * This will result in a request to the server.
     */
    export let write;

    let name = reference.name;
    let choices = Object.keys(reference.choices);
    let expanded = false;

    function handleToggleExpand() {
        expanded = !expanded;
    }

    function handleValueChange(v) {
        // Check if value is a choice
        if (!choices.includes(v)) {
            return;
        }

        reference.active = v;
        write();
    }
</script>

<div class="setting">
    <div class="choice">
        <div on:click={handleToggleExpand} class:expanded={expanded} class="name">{name} - {reference.active}</div>
        {#if expanded}
            <div class="values" transition:slide|local={{duration: 200, easing: sineInOut}}>
                {#each choices as v}
                    <div class="value" on:click={() => handleValueChange(v)} class:enabled={v === reference.active}>{v}</div>
                {/each}
            </div>
        {/if}
    </div>

    {#if reference.choices[reference.active].value.length > 0}
      <div class="settings" transition:fade|local={{duration: 200, easing: sineInOut}}>
        {#each reference.choices[reference.active].value as s}
            <GenericSetting reference={s} write={write} />
        {/each}
      </div>
    {/if}
</div>

<style lang="scss">
  .settings {
    background-color: rgba(0, 0, 0, 0.36);
    border-right: solid 4px var(--accent);
    overflow: hidden;
    margin-top: 10px;
  }

  .setting {
    overflow: hidden;
  }

  .choice {
    padding: 7px 10px;
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
      color: var(--textdimmed);
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
