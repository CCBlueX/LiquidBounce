<script>
    /**
     * A reference to the value instance of this setting. It is part of the module configurable and should NOT lose its reference.
     */
    export let reference;
    /**
     * This function is passed from the parent component and is used to write the new configurable to the client.
     * This will result in a request to the server.
     */
    export let write;
</script>

<div class="setting">
    <label class="switch">
        <input type="checkbox" bind:checked={reference.value} on:change={write}/>
        <span class="slider"/>

        <div class="name">{reference.name}</div>
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
  }

  .switch input:checked + .slider:before {
    transform: translateX(10px);
    background-color: var(--accent);
  }
</style>
