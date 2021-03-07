<script>
    export let name;
</script>

<div class="module">
    <div class="unskew">{name}</div>
</div>

<style>
    .module {
        background-color: rgba(0, 0, 0, 0.68);
        color: white;
        font-size: 14px;
        border-radius: 4px;
        transform: matrix(1, 0, -0.09, 1, 0, 0);
        padding: 5px 15px 5px 8px;
        border-left: solid 4px #4677FF;
        text-align: right;
        width: max-content;
    }

    .unskew {
        transform: skew(5deg, 0);
        text-transform: capitalize;
    }
</style>