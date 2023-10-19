<script>
    export let instance;

    let name = instance.getName();
    let value = instance.get();

    function handleValueChange() {
        instance.set(value);
    }
</script>

<div class="setting">
    <label class="switch">
        <input type="checkbox" bind:checked={value} on:change={handleValueChange}/>
        <span class="slider"/>

        <div class="name">{name}</div>
    </label>
</div>

<style lang="scss">
  .setting {
    padding: 7px 10px;
    display: grid;
    grid-template-columns: max-content auto;
    align-items: center;
    column-gap: 10px;
  }

  .name {
    font-weight: 500;
    color: white;
    font-size: 12px;
    margin-left: 30px;
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

  .switch {
    position: relative;
    display: inline-block;
    width: 22px;
    height: 12px;
  }

  .switch input {
    display: none;
  }

  .switch input:checked + .slider {
    background-color: var(--accent);
    filter: brightness(0.4);
  }

  .switch input:checked + .slider:before {
    transform: translateX(10px);
    background-color: var(--accent);
  }
</style>
