<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let value: boolean;
    export let title: string;

    const dispatch = createEventDispatcher();
</script>

<div class="switch-setting">
    <label class="switch">
        <input type="checkbox" bind:checked={value} on:change={() => dispatch("change")}/>
        <span class="slider"></span>
    </label>

    <div class="title">{title}</div>
</div>
<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as *;

  .switch-setting {
    display: flex;
    align-items: center;
  }

  .title {
    color: $menu-text-color;
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
    background-color: color.adjust($menu-text-color, $lightness: -55%);
    transition: ease 0.4s;
    height: 14px;
    border-radius: 10px;

    &::before {
      position: absolute;
      content: "";
      height: 21px;
      width: 21px;
      top: -4px;
      left: -10px;
      background-color: $menu-text-color;
      transition: ease 0.4s;
      border-radius: 50%;
    }
  }

  .switch {
    position: relative;
    display: flex;
    width: 28px;
    height: 18px;
    align-items: center;
    cursor: pointer;
    margin: 0 10px;

    input {
      display: none;
    }

    input:checked + .slider {
      background-color: color.adjust($accent-color, $saturation: -60%, $lightness: -15%);
    }

    input:checked + .slider:before {
      transform: translateX(27px);
      background-color: $accent-color;
    }
  }
</style>
