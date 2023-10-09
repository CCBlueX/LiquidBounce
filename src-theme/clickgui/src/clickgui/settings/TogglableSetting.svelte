<script>
    import {sineInOut} from "svelte/easing";
    import {fade} from "svelte/transition";
    import GenericSetting from "./GenericSetting.svelte";

    export let instance;

    const hiddenSettings = ["Enabled", "Hidden", "Bind"];

    function toJavaScriptArray(a) {
        const v = [];
        for (let i = 0; i < a.length; i++) {
            if (!hiddenSettings.includes(a[i].getName())) {
                v.push(a[i]);
            }
        }

        return v;
    }

    let value = instance.getEnabledValue().get();
    let name = instance.getName();

    let settings = toJavaScriptArray(instance.getContainedValues());

    function setTogglableValue(e) {
        instance.getEnabledValue().set(value);
        settings = toJavaScriptArray(instance.getContainedValues());
    }
</script>

<div class="setting">
    <div class="head">
        <div class="boolean">
            <label class="switch">
                <input type="checkbox" bind:checked={value} on:change={setTogglableValue}/>
                <span class="slider"/>

                <div class="name">{name}</div>
            </label>
        </div>
    </div>

    {#if value}
        <div class="settings" transition:fade|local={{duration: 200}}>
            {#each settings as s}
                <GenericSetting instance={s}/>
            {/each}
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
    color: var(--text);;
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
    filter: brightness(70%);
  }

  .switch input:checked + .slider:before {
    transform: translateX(10px);
    background-color: var(--accent);
  }
</style>
