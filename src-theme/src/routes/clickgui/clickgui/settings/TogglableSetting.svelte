<script>
  import { sineInOut } from "svelte/easing";
  import { fade } from "svelte/transition";
  import GenericSetting from "./GenericSetting.svelte";
  import SubSettings from "./SubSettings.svelte";

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
  let enabled = reference.value.find((v) => v.name === "Enabled").value;

  function changed() {
    // Pass the new value to the instance
    for (const v of reference.value) {
      if (v.name === "Enabled") {
        v.value = enabled;
      }
    }

    write();
  }
</script>

<div class="setting">
  <div class="head">
    <div class="boolean">
      <label class="switch">
        <input type="checkbox" bind:checked={enabled} on:change={changed} />
        <span class="slider" />

        <div class="name">{name}</div>
      </label>
    </div>
  </div>

  {#if enabled}
    <div class="settings" transition:fade|local={{ duration: 200 }}>
      {#if s.name !== "Enabled"}
        <SubSettings
          settings={reference.value}
          validator={(s) => {
            s.name !== "Enabled";
          }}
        />
      {/if}
    </div>
  {/if}
</div>

<style lang="scss">
  .settings {
    background-color: rgba(0, 0, 0, 0.36);
    border-right: solid 4px var(--accent);
    overflow: hidden;
  }

  .boolean {
    padding: 7px 10px;
    display: grid;
    grid-template-columns: max-content auto;
    align-items: center;
    column-gap: 10px;
  }

  .name {
    font-weight: 500;
    color: var(--text);
    font-size: 12px;
    margin-left: 30px;
  }

  .switch {
    position: relative;
    display: inline-block;
    width: 22px;
    height: 12px;
  }

  .slider {
    position: absolute;
    top: 2px;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #707070;
    transition: ease 0.4s;
    height: 8px;
    border-radius: 4px;

    &::before {
      position: absolute;
      content: "";
      height: 12px;
      width: 12px;
      top: -2px;
      left: 0;
      background-color: white;
      transition: ease 0.4s;
      border-radius: 50%;
    }
  }

  .switch input {
    display: none;
  }

  .switch input:checked + .slider {
    background-color: var(--accent);
  }

  .switch input:checked + .slider:before {
    transform: translateX(10px);
    background-color: var(--accent);
  }
</style>
