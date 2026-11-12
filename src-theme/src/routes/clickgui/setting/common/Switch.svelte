<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let value: boolean;
    export let name: string | null = null;

    const dispatch = createEventDispatcher();
</script>

<label class="switch">
    <input type="checkbox" bind:checked={value} on:change={() => dispatch("change")}/>
    <span class="track"></span>

    {#if name}
        <span class="name">{name}</span>
    {/if}
</label>

<style lang="scss">
  .switch {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    // Room for the thumb, which is taller than the track
    padding: 1px;
    cursor: pointer;
  }

  input {
    position: absolute;
    opacity: 0;
    pointer-events: none;
  }

  .track {
    position: relative;
    flex: none;
    width: 28px;
    height: 14px;
    border-radius: 7px;
    background-color: var(--clickgui-switch-track-color);
    transition: ease background-color .15s;

    &::after {
      content: "";
      position: absolute;
      top: -1px;
      left: -1px;
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background-color: var(--clickgui-switch-thumb-color);
      transition: ease transform .15s, ease background-color .15s;
    }
  }

  input:checked + .track {
    background-color: var(--clickgui-switch-track-active-color);

    &::after {
      transform: translateX(14px);
      background-color: var(--clickgui-switch-thumb-active-color);
    }
  }

  input:focus-visible + .track {
    outline: 2px solid var(--accent-color);
    outline-offset: 3px;
  }

  .name {
    font-weight: 500;
    font-size: 12px;
    color: var(--clickgui-text-color);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
</style>
