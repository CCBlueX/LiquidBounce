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

    let name = reference.name;
</script>

<div class="setting">
    <div class="name">{name}</div>
    <input type="text" bind:value={reference.value} on:change={write} placeholder={name}/>
</div>

<style lang="scss">
  .setting {
    padding: 7px 10px;
  }

  .name {
    font-weight: 500;
    color: var(--text);;
    font-size: 12px;
  }

  input {
    width: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    border: none;
    font-family: "Montserrat", sans-serif;
    padding: 5px;
    border-bottom: solid 2px transparent;
    border-radius: 5px;
    font-size: 12px;
    margin-top: 5px;
    color: var(--text);;
    transition: ease border-bottom 0.2s;
    border: solid 2px transparent;

    &:focus {
      border: solid 2px var(--accent);
    }
  }
</style>
