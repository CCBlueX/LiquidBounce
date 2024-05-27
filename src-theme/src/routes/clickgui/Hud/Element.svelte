<script lang="ts">
  export let scaleFactor: number;
  export let element: {
    name: string;
    height: number;
    width: number;
    x: number;
    y: number;
  };

  let startX = 0;
  let startY = 0;
  function onMouseDown(e: MouseEvent) {
    console.log(e);

    startX = e.clientX;
    startY = e.clientY;

    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", () => {
      document.removeEventListener("mousemove", onMouseMove);
    });
  }

  function onMouseMove(e: MouseEvent) {
    element.x = Math.max(
      Math.min(
        element.x + e.movementX * (2 / scaleFactor),
        window.innerWidth - element.width,
      ),
      0,
    );
    element.y = Math.max(
      Math.min(
        element.y + e.movementY * (2 / scaleFactor),
        window.innerHeight - element.height,
      ),
      0,
    );
  }

  function onResizeDown(e: MouseEvent) {
    console.log("resize down");
    document.addEventListener("mousemove", onResizeMove);
    document.addEventListener("mouseup", () => {
      document.removeEventListener("mousemove", onResizeMove);
    });
  }

  function onResizeMove(e: MouseEvent) {
    console.log("resize move");

    element.width += e.movementX;
    element.height += e.movementY;
  }
</script>

<button
  on:mousedown={onMouseDown}
  class="element"
  style="width: {element.width}px; height: {element.height}px; top: {element.y}px; left: {element.x}px;"
>
  <button class="resize-handle" on:mousedown|stopPropagation={onResizeDown}
  ></button>
</button>

<style lang="scss">
  @import "../../../colors.scss";

  .element {
    position: absolute;
    background-color: rgba($clickgui-base-color, 0.3);
    border: 2px solid $accent-color;
    border-radius: 10px 10px 0 10px;
    display: grid;
    place-items: end end;
  }

  .resize-handle {
    border: none;
    background-color: $accent-color;
    cursor: nwse-resize;

    height: 10px;
    width: 10px;
    right: 0;
    bottom: 0;
    transform: translate(50%, 50%);
  }
</style>
