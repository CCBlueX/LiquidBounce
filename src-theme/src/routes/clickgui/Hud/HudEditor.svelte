<script lang="ts">
  import Element from "./Element.svelte";

  export let scaleFactor;

  let elements = [
    {
      name: "HudEditor",
      height: 100,
      width: 200,
      heightRange: [50, 200] as [number, number],
      widthRange: [100, 400] as [number, number],
      x: 100,
      y: 100,
      variableRatio: false,
    },
  ];
</script>

<div class="hud-editor">
  {#each elements as element}
    <Element {element} {scaleFactor} />
  {/each}
</div>
