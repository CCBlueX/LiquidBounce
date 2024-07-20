<script lang="ts">
  export let scaleFactor: number;
  export let element: {
    name: string;
    height: number;
    width: number;
    heightRange: [number, number];
    widthRange: [number, number];
    x: number;
    y: number;
    variableRatio: boolean;
  };

  let startX = 0;
  let startY = 0;

  const ratio = element.height / element.width;
  console.log(element.height, element.width, ratio);

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
      Math.min(element.x + e.movementX, window.innerWidth - element.width),
      0,
    );
    element.y = Math.max(
      Math.min(element.y + e.movementY, window.innerHeight - element.height),
      0,
    );
  }

  const onResizeDown = (x: number, y: number) => (e: MouseEvent) => {
    console.log("resize down");
    const resizeFunction = onResizeMove(x, y);
    document.addEventListener("mousemove", resizeFunction);
    document.addEventListener("mouseup", () => {
      document.removeEventListener("mousemove", resizeFunction);
    });
  };

  function updHeight(movementY: number, direction: number) {
    if (direction === 0) return;
    element.height = Math.max(
      Math.min(element.height + movementY * direction, element.heightRange[1]),
      element.heightRange[0],
    );
  }
  function updWidth(movementX: number, direction: number) {
    if (direction === 0) return;
    element.width = Math.max(
      Math.min(element.width + movementX * direction, element.widthRange[1]),
      element.widthRange[0],
    );
  }

  const onResizeMove = (x: number, y: number) => (e: MouseEvent) => {
    console.log("resize move");
    const prevWidth = element.width;
    const prevHeight = element.height;
    if (element.variableRatio) {
      updHeight(e.movementY, y);
      updWidth(e.movementX, x);
    } else {
      const avg = e.movementX * ratio * x + e.movementY * y;
      updWidth(avg, 1);
      element.height = element.width * ratio;
      console.log("rat", ratio);
      console.log(element.width == element.height * ratio);
    }

    if (x < 0) {
      element.x += prevWidth - element.width;
    }

    if (y < 0) {
      element.y += prevHeight - element.height;
    }
  };
</script>

<button
  on:mousedown={onMouseDown}
  class="element"
  style="width: {element.width * (2 / scaleFactor)}px; height: {element.height *
    (2 / scaleFactor)}px; top: {element.y *
    (2 / scaleFactor)}px; left: {element.x * (2 / scaleFactor)}px;"
>
  <!-- Resize handle bars -->
  <button
    class="resize-handle ew right-0 top-0 h-full"
    on:mousedown|stopPropagation={onResizeDown(1, 0)}
  ></button>
  <button
    class="resize-handle ew left-0 top-0 h-full"
    on:mousedown|stopPropagation={onResizeDown(-1, 0)}
  ></button>
  <button
    class="resize-handle ns left-0 top-0 w-full"
    on:mousedown|stopPropagation={onResizeDown(0, -1)}
  ></button>
  <button
    class="resize-handle ns left-0 bottom-0 w-full"
    on:mousedown|stopPropagation={onResizeDown(0, 1)}
  ></button>
  <!-- Resize Handel points -->
  <button
    class="resize-handle left-0 top-0 nwse"
    on:mousedown|stopPropagation={onResizeDown(-1, -1)}
  ></button>
  <button
    class="resize-handle right-0 top-0 nesw"
    on:mousedown|stopPropagation={onResizeDown(1, -1)}
  ></button>
  <button
    class="resize-handle left-0 bottom-0 nesw"
    on:mousedown|stopPropagation={onResizeDown(-1, 1)}
  ></button>
  <button
    class="resize-handle right-0 bottom-0 nwse"
    on:mousedown|stopPropagation={onResizeDown(1, 1)}
  ></button>
</button>

<style lang="scss">
  @import "../../../colors.scss";

  .element {
    position: absolute;
    background-color: rgba($clickgui-base-color, 0.2);
    border: 2px solid rgba($accent-color, 0.8);
    border-radius: 7px;
    cursor: grab;
    // display: grid;
    // place-items: end end;
  }

  .resize-handle {
    border: none;
    // background-color: $accent-color;
    background-color: transparent;

    position: absolute;
    height: 10px;
    width: 10px;
    // transform: translate(50%, 50%);
  }

  .right-0 {
    right: -5px;
  }
  .left-0 {
    left: -5px;
  }

  .top-0 {
    top: -5px;
  }

  .bottom-0 {
    bottom: -5px;
  }

  .h-full {
    height: calc(100% + 10px);
  }
  .w-full {
    width: 100%;
  }

  .ns {
    cursor: ns-resize;
  }
  .ew {
    cursor: ew-resize;
  }

  .nwse {
    cursor: nwse-resize;
  }
  .nesw {
    cursor: nesw-resize;
  }
</style>
