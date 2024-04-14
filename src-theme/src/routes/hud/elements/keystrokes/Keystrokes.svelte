<script>
    import Key from "./Key.svelte";
</script>

<div class="keystrokes">
    <Key key="W" gridArea="a" />
    <Key key="A" gridArea="b" />
    <Key key="S" gridArea="c" />
    <Key key="D" gridArea="d" />
    <Key key="Space" gridArea="e" />
</div>

<style lang="scss">
    .keystrokes {
      display: grid;
      grid-template-areas:
        ". a ."
        "b c d"
        "e e e";
      grid-template-columns: repeat(3, 50px);
      gap: 5px;
    }
</style>