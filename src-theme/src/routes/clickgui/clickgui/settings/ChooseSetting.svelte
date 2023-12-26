<script>
    import {sineInOut} from "svelte/easing";
    import {slide} from "svelte/transition";

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
    let choices = reference.choices;

    let expanded = false;

    /**
     * This function handles the toggling of the expanded state.
     */
    function handleToggleExpand() {
        expanded = !expanded;
    }

    /**
     * Since we have a list of choices down below to choose from, we need to have a function that handles the change and writes it
     * directly to the instance.
     * 
     * @param choice The choice to change to (e.g. "Rainbow")
     */
    function handleValueChange(choice) {
        reference.value = choice;
        write();
    }
</script>

<div class="setting">
    <div on:click={handleToggleExpand} class:expanded={expanded} class="name">{name} - {reference.value}</div>
    {#if expanded}
        <div class="values" transition:slide|local={{duration: 200, easing: sineInOut}}>
            {#each choices as choice}
                <div class="value" on:click={() => handleValueChange(choice)} class:enabled={choice === reference.value}>{choice}</div>
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
