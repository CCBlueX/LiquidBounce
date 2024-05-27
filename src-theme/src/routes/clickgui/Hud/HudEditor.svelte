<script>
  import Element from "./Element.svelte";

  export let scaleFactor;

  let elements = [
    {
      name: "HudEditor",
      height: 100,
      width: 200,
      x: 100,
      y: 100,
    },
  ];
</script>

<div class="hud-editor">
  {#each elements as element}
    <Element {element} {scaleFactor} />
  {/each}
</div>
