<script lang="ts">
    let left = -100;
    setInterval(() => {
        left += 100;

        if (left > window.innerWidth + 100) {
            left = -100;
        }
    }, 500);
</script>

<div class="taco">
    <img src="img/hud/taco/taco.gif" alt="taco no load :(("
         class:transition={left < window.innerWidth + 100 && left > -100} style="left: {left}px">
</div>

<style lang="scss">
  .taco {
    position: relative;
    height: 60px;
    background-color: red;

    img {
      position: absolute;
      height: 100%;

      &.transition {
        transition: linear left 500ms;
      }
    }
  }
</style>

