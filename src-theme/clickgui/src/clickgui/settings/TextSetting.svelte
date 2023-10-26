<script>
    export let instance;

    let name = instance.getName();
    let value = instance.get();

    function handleTextChange(e) {
        instance.set(value);
    }
</script>

<div class="setting">
    <div class="name">{name}</div>
    <input type="text" bind:value={value} on:change={handleTextChange} placeholder={name}/>
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
