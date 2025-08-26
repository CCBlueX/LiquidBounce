<script lang="ts">
    import {fade} from "svelte/transition";

    const titles: string[] = ["京东白条",];
    const descriptions1: string[] = [
        "许锦良要买 iPhone 16 Pro Max，你买不买？你死也得买",

    ];
    const descriptions2: string[] = [
        "那买完了京东白条还不上了怎么办？不可能还不上，他连逾期都不是，只是严重警告你同学不能再用你妈的京东白条贷款",
    ];

    function wrapTitle(str: string, title: string): string {
        return str.replace(new RegExp(title, "g"), `「${title}」`);
    }

    let currentTitle = $state("");
    let currentDesc1 = $state("");
    let currentDesc2 = $state("");

    function refreshContent(): void {
        const title = titles[Math.floor(Math.random() * titles.length)];
        const desc1 = descriptions1[Math.floor(Math.random() * descriptions1.length)];
        const desc2 = descriptions2[Math.floor(Math.random() * descriptions2.length)];

        currentTitle = title;
        currentDesc1 = wrapTitle(desc1, title);
        currentDesc2 = wrapTitle(desc2, title);
    }

    refreshContent();
</script>


<div
        class="splash-tip"
        role="button"
        tabindex="0"
        onclick={refreshContent}
        onkeydown={(e) => e.key === 'Enter' && refreshContent()}
        transition:fade={{ duration: 600 }}
>
    <h1>{currentTitle}</h1>
    <p class="desc1">{currentDesc1}。</p>
    <p class="desc2">{currentDesc2}···</p>
</div>

<style>
    .splash-tip {
        position: fixed;
        left: 50%;
        top: 75%;
        -webkit-font-smoothing: none;
        -moz-osx-font-smoothing: grayscale;
        transform: translate(-50%, -50%);
        display: flex;
        font-family: 'Genshin', serif;
        flex-direction: column;
        align-items: center;
        text-align: center;
        white-space: nowrap;
        color: #C5AF89;
        width: 100%;
        gap: 6px;
        z-index: 1000;
    }

    h1 {

        font-size: 36px;
        margin-bottom: 20px;
    }

    .desc1 {
        font-size: 28px;
        line-height: 1.5;
        margin: 10px 0;

    }

    .desc2 {

        font-size: 28px;
        line-height: 1.5;


    }

    @media screen and (max-width: 1600px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.9);
        }
    }

    @media screen and (max-width: 1366px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.8);
        }
    }

    @media screen and (max-width: 1200px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.7);
        }
    }

    @media screen and (max-height: 1100px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.6);
        }
    }

    @media screen and (max-height: 700px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.5);
        }
    }

</style>
