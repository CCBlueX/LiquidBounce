<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let value: boolean;
    export let title: string;

    const dispatch = createEventDispatcher();
</script>

<label class="switch-setting">
    <span class="switch">
        <input type="checkbox" bind:checked={value} on:change={() => dispatch("change")}/>
        <span class="slider"></span>
    </span>

    <span class="title">{title}</span>
</label>

<style lang="scss">

  .switch-setting {
    display: flex;
    align-items: center;
    cursor: pointer;
  }

  .title {
    color: var(--menu-text-color);
    font-size: 20px;
    margin-left: 10px;
    font-weight: 500;
  }

  .slider {
    position: absolute;
    top: 2px;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: var(--menu-switch-track-color);
    transition: background-color var(--duration-fast) var(--ease-standard);
    height: 14px;
    border-radius: var(--radius-pill);

    &::before {
      position: absolute;
      content: "";
      height: 21px;
      width: 21px;
      top: -4px;
      left: -10px;
      background-color: var(--menu-switch-thumb-color);
      transition: transform var(--duration-fast) var(--ease-emphasized),
      background-color var(--duration-fast) var(--ease-standard),
      box-shadow var(--duration-fast) var(--ease-standard);
      border-radius: 50%;
    }
  }

  .switch {
    position: relative;
    display: flex;
    width: 28px;
    height: 18px;
    align-items: center;
    margin: 0 10px;

    input {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      margin: 0;
      opacity: 0;
      cursor: pointer;
    }

    input:focus-visible + .slider {
      outline: var(--focus-ring-width) solid var(--accent-color);
      outline-offset: var(--focus-ring-offset);
    }

    input:checked + .slider {
      background-color: var(--menu-switch-track-active-color);
    }

    input:checked + .slider:before {
      transform: translateX(27px);
      background-color: var(--menu-switch-thumb-active-color);
      box-shadow: 0 0 0 3px var(--accent-subtle-background-color);
    }
  }
</style>
