<div class="loader-wrapper">
    <div class="lds-ring">
        <div></div>
        <div></div>
        <div></div>
        <div></div>
    </div>
</div>

<style lang="scss">
  /*
    Taken from https://loading.io/css/
   */

  @use "../../../colors.scss" as *;

  .loader-wrapper {
    position: absolute;
    top: 50%;
    left: 15px;
    transform: translateY(-50%);
    width: 32px;
    height: 32px;
  }

  .lds-ring,
  .lds-ring div {
    box-sizing: border-box;
    position: relative;
    color: $menu-button-loader-color;
  }

  .lds-ring {
    display: inline-block;
    width: 32px;
    height: 32px;
  }

  .lds-ring div {
    box-sizing: border-box;
    display: block;
    position: absolute;
    width: 25px;
    height: 25px;
    margin: 4px;
    border: 4px solid currentColor;
    border-radius: 50%;
    animation: lds-ring 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
    border-color: currentColor transparent transparent transparent;
  }

  .lds-ring div:nth-child(1) {
    animation-delay: -0.45s;
  }

  .lds-ring div:nth-child(2) {
    animation-delay: -0.3s;
  }

  .lds-ring div:nth-child(3) {
    animation-delay: -0.15s;
  }

  @keyframes lds-ring {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }
</style>