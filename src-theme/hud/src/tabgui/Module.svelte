<script>
    import {afterUpdate} from 'svelte';

    export let name;
    export let active;
    export let enabled;

    let el;

    afterUpdate(() => {
        if (active) {
            el.scrollIntoView({block: "start", behavior: "smooth"});
        }
    });
</script>

<div bind:this={el} class="module" class:active class:enabled>
    <div class="name">{name}</div>
</div>

<style lang="scss">
  .module {
    font-weight: 600;
    color: #CBD1E3;
    font-size: 12px;
    padding: 6px 15px 6px 10px;
    transition: ease color .2s;

    .name {
      transition: ease transform .2s;
    }

    &.active {
      background-color: rgba(0, 0, 0, 0.36);

      .name {
        transform: translateX(5px);
      }
    }

    &.enabled {
      color: white;
    }
  }
</style>
