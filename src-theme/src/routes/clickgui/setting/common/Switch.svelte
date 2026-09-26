<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let value: boolean;
    export let name: string;

    const dispatch = createEventDispatcher();
</script>

<label class="switch-container">
    <span class="switch">
        <input type="checkbox" bind:checked={value} on:change={() => dispatch("change")}/>
        <span class="slider"></span>
    </span>

    <span class="name">{name}</span>
</label>

<style lang="scss">

  .switch-container {
    display: flex;
    align-items: center;
    cursor: pointer;
  }

  .name {
    font-weight: 500;
    color: var(--clickgui-text-color);
    font-size: 12px;
    margin-left: 7px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .slider {
    position: absolute;
    top: 2px;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: var(--clickgui-switch-track-color);
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
      background-color: var(--clickgui-switch-thumb-color);
      transition: ease 0.4s;
      border-radius: 50%;
    }
  }

  .switch {
    position: relative;
    width: 22px;
    height: 12px;

    input {
      display: none;
    }

    input:checked + .slider {
      background-color: var(--clickgui-switch-track-active-color);
    }

    input:checked + .slider:before {
      transform: translateX(10px);
      background-color: var(--clickgui-switch-thumb-active-color);
    }
  }
</style>
