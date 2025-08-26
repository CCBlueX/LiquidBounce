<script lang="ts">
    import {fade} from "svelte/transition";

    export let name: string;
    export let selected: boolean;
</script>

<div class="category" class:selected>
    <div class="background" class:selected></div>
    <div class="icon" class:selected>
        <img
                alt="icon"
                src="img/hud/tabgui/{name.toLowerCase()}.svg"
                transition:fade={{ duration: 200 }}
        />
    </div>
    <div class="name">
        {name}
    </div>
</div>


<style lang="scss">
  @use "../../../../colors.scss" as *;

  .category {
    position: relative;
    overflow: hidden;
    display: flex;
  }

  .name {
    position: relative;
    z-index: 1;
    font-weight: 500;
    color: $text;
    font-size: 16px;
    width: 100%;
    padding: 7px 12px 7px 12px;
    transition: all 0.2s ease-out;
    overflow: hidden;
  }

  .background {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(to right, rgba($base, 0.5) 0%, color-mix(in srgb, var(--primary-color) 40%, transparent) 100%);
    transform: translateX(-100%);
    transition: transform 0.3s ease-out;
  }

  .background.selected {
    transform: translateX(0);
  }


  .icon {
    width: 62px;
    position: relative;

    img {
      position: absolute;
      top: 0;
      bottom: 0;
      left: 0;
      right: 0;
      margin: auto;
      width: 24px;
      height: 24px;
      filter: brightness(0) invert(1);
    }

    &.selected {

      background: linear-gradient(135deg, color-mix(in srgb, var(--primary-color) 20%, transparent) 0%,
              color-mix(in srgb, var(--secondary-color) 20%, transparent) 100%);

      img {

        mix-blend-mode: screen;
        filter: none;
      }
    }
  }
</style>
