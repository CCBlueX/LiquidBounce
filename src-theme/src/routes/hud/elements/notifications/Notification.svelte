<script lang="ts">
    export let title: string;
    export let message: string;
    export let severity: string;
</script>

<div class="notification">
    <div class="icon {severity.toString().toLowerCase()}" />
    <div class="title">{title}</div>
    <div class="message">{message}</div>
</div>

<style lang="scss">
    .notification {
        display: grid;
        grid-template-areas:
            "a b"
            "a c";
        grid-template-columns: max-content 1fr;
        column-gap: 10px;
        background: rgba(0, 0, 0, 0.68);
        border-radius: 5px;
        width: 300px;
        overflow: hidden;
        padding: 10px;
        margin-bottom: 10px;
    }

    .icon {
        height: 40px;
        width: 40px;
        background-position: center;
        background-repeat: no-repeat;
        border-radius: 4px;
        grid-area: a;

        &.success {
            background-color: #4dac68;
            background-image: url("img/hud/notification/icon-success.svg");
        }

        &.error {
            background-color: #fc4130;
            background-image: url("img/hud/notification/icon-error.svg");
        }

        &.info {
            background-color: #4677ff;
            background-image: url("img/hud/notification/icon-info.svg");
        }

        &.enabled {
            background-color: #4dac68;
            background-image: url("img/hud/notification/icon-enabled.svg");
        }

        &.disabled {
            background-color: #fc4130;
            background-image: url("img/hud/notification/icon-disabled.svg");
        }
    }

    .title {
        grid-area: b;
        font-size: 14px;
        color: white;
        font-weight: 600;
    }

    .message {
        grid-area: c;
        font-size: 12px;
        color: #cbd1e3;
    }
</style>
