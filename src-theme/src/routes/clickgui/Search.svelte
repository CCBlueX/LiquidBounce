<script lang="ts">
    import type {ConfigurableSetting, Module} from "../../integration/types";
    import {getModuleSettings, setModuleEnabled} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {ClickGuiValueChangeEvent, KeyboardKeyEvent, ModuleToggleEvent} from "../../integration/events";
    import {highlightModuleName, filteredModules, showSearch, query} from "./clickgui_store";
    import {convertToSpacedString, spaceSeperatedNames} from "../../theme/theme_config";
    import {shrinkOut} from "../../util/animate_utils";
    import {fly, slide} from "svelte/transition";
    import {quintOut} from 'svelte/easing';
    import {onMount, onDestroy, tick} from "svelte";
    import {showResults} from "./clickgui_store";
    import {get} from "svelte/store";
    import {writable} from 'svelte/store';
    import {Howl} from "howler";

    export let modules: Module[];
    let isSearchFocused = false;
    let resultElements: HTMLElement[] = [];
    let searchContainerElement: HTMLElement;
    let searchInputElement: HTMLInputElement;
    let lastClickTime = 0;
    let localFiltered: Module[] = [];
    let selectedIndex = -1;
    let hasContent = false;
    let forcedShow = false;
    let autoFocus: boolean = true;
    let clickOutsideCount = 0;
    let placeholder = "";
    let showHistory = false;
    let isCialloActive = false;
    let interval: ReturnType<typeof setInterval>;

    $: if ($showSearch && autoFocus) {
        tick().then(() => {
            searchInputElement?.focus();
        });
    }
    $: if (!$showSearch) {
        $query = '';
    }

    function handleFocus() {
        placeholder = "Search (A-Z only)";

        if ($query.length > 0) {
            setTimeout(() => {
                if (document.activeElement === searchInputElement) {
                    (searchInputElement as HTMLInputElement).select();
                }
            }, 0);
        }
    }

    const recentPlaceholders: string[] = [];
    const maxRecent = 5;
    const options = [
        {text: "Ciallo~(∠・ω< )⌒★", weight: 2},//柚子厨真恶心
        {text: "世界には多くの面白い事がありますが、一方的にそれらを探求しても幸せにはなれません，", weight: 1}, // Einstein携愛敬上
        {text: "相互理解があってこそ、存在の概念が生まれます。", weight: 1}, // Einstein携愛敬上
        {text: "人は幸せになる為に生きている。それがどんな形であれ、幸せを求める事に罪はない。", weight: 1}, // CLANNAD
        {text: "大切なのは、過去じゃない。今、この瞬間をどう生きるかだ。", weight: 1}, // Fate/stay night
        {text: "願いは叶えるもの。叶わないなら、それは願いじゃない。", weight: 1}, // 魔法少女まどか☆マギカ
        {text: "この手が汚れてもいい。あなただけは守りたい。", weight: 1}, // Rewrite
        {text: "奇跡は誰にでも起こるものじゃない。奇跡を起こすんだ。", weight: 1}, // Steins;Gate
        {text: "涙の数だけ強くなれるよ。", weight: 1}, // Air
        {text: "一番つらいのは、忘れようとすることだよ。", weight: 1}, // Kanon
        {text: "ものの見方は自分で変わりますし、美しいも醜いも解釈次第です。", weight: 1}, // U-ena-空焰火少女
        {text: "約束は、未来へと続く道しるべ。", weight: 1}, // Little Busters!
        {text: "偽物だって、本物になり得る。", weight: 1}, // 月姫
        {text: "優しさは時として、残酷なものだ。", weight: 1}, // 沙耶の唄
        {text: "人は皆、誰かの英雄になれる。", weight: 1}, // Angel Beats!
        {text: "知ってることに罪はないけど、知ってて何もしないのは否定できない罪だよね。", weight: 1},//秽翼的尤斯蒂娅
        {text: "過去は変えられない。でも未来は変えられる。", weight: 1}, // Chaos;Child
        {text: "愛とは、相手を理解しようとする努力のことだ。", weight: 1}, // 白色相簿2
        {text: "傷つくことを恐れてたら、本当に大切なものまで失ってしまう。", weight: 1}, // CLANNAD
        {text: "世界は美しくなんかない。そしてそれ故に、美しい。", weight: 1}, // 鬼哭街
        {text: "誰かのために生きることは、自分自身を生きることでもある。", weight: 1}, // Summer Pockets
        {text: "幸せとは、気付くものであって、探すものではない。", weight: 1}, // ハルカナソラ
        {text: "人は忘れることで前に進める。でも、忘れてはいけないこともある。", weight: 1}, // メモリーズオフ
        {text: "本当の強さとは、弱さを認めることだ。", weight: 1}, // グリザイアの果実
        {text: "出会いには必ず意味がある。たとえそれが別れで終わっても。", weight: 1}, // サクラの詩
        {text: "未来は一つじゃない。無数に枝分かれしている。", weight: 1}, // Ever17
        {text: "愛してるとは、共に生きるということ。", weight: 1}, // 水仙
        {text: "傷ついた心は、優しさでしか癒せない。", weight: 1}, // To Heart
        {text: "夢を見ることは、現実を変える第一歩だ。", weight: 1}, // バルドスカイ
        {text: "孤独とは、心の隙間を埋めるものがない状態だ。", weight: 1}, // 素晴らしき日々
        {text: "真実はいつも一つとは限らない。", weight: 1}, // Remember11
        {text: "人は変わることができる。それが人間の素晴らしさだ。", weight: 1}, // G弦上の魔王
        {text: "大切なものは、失って初めてその価値に気づく。", weight: 1}, // 秋の回想
        {text: "希望とは、自分で作り出すものだ。", weight: 1}, // 装甲悪鬼村正
        {text: "誰もが星の欠片を持っている。光り続けるために。", weight: 1}, // 星の夢
        {text: "過去に囚われるな。未来を恐れるな。今を生きろ。", weight: 1}, // Dies irae
        {text: "愛とは、相手の幸せを願うことだ。", weight: 1}, // 君の名残は静かに揺れて
        {text: "涙は弱さじゃない。本当の強さの証だ。", weight: 1}, // この大空に、翼をひろげて
        {text: "人は皆、不完全だから美しい。", weight: 1}, // 穢翼のユースティア
        {text: "信じることは、時として奇跡を起こす。", weight: 1}, // 11eyes
        {text: "絆とは、離れていても心がつながっていること。", weight: 1}, // 遥かに仰ぎ、麗しの
        {text: "本当の自分とは、自分で決めるものだ。", weight: 1}, // サクラノ詩
        {text: "命は有限だからこそ輝く。", weight: 1}, // 腐り姫
        {text: "優しさは、人を救う最大の武器だ。", weight: 1}, // はつゆきさくら
        {text: "世界は残酷だ。でも美しい。", weight: 1}, // 鬼哭街
        {text: "未来は変えられる。変えようと思えば。", weight: 1}, // Robotics;Notes
        {text: "孤独は、人を強くもするし、脆くもする。", weight: 1}, // 素晴らしき日々
        {text: "夢は逃げない。逃げるのはいつも自分だ。", weight: 1}, // バルドスカイ
        {text: "真実は時として、人を傷つける。", weight: 1}, // ひぐらしのなく頃に
        {text: "人は皆、誰かを救うために生まれてくる。", weight: 1}, // Angel Beats!
        {text: "愛とは、相手の全てを受け入れること。", weight: 1}, // 月姫
        {text: "傷は癒える。でも、傷跡は残る。それが生きるということだ。", weight: 1}, // Rewrite
        {text: "希望は、絶望の向こう側にある。", weight: 1}, // ダンガンロンパ
        {text: "人は皆、光と影を持っている。", weight: 1}, // シュタインズ・ゲート
        {text: "一番大切なものは、目に見えない。", weight: 1}, // Air
        {text: "生きる意味は、自分で見つけるものだ。", weight: 1}, // CLANNAD
        {text: "世界は広い。でも、大切な場所はきっと近くにある。", weight: 1}, // サマーポケッツ
        {text: "信じ続けることが、奇跡を呼ぶ。", weight: 1}, // リトルバスターズ!
        {text: "涙は心の雨。やがて虹が架かる。", weight: 1},// Kanon
        {text: "空気の重み、胸の震え", weight: 1}, // 魔法使いの夜
        {text: "光が先を行き、影が後に続く。鳥は空を翔ける", weight: 1}, // 魔法使いの夜
        {text: "魚は海を跳ねる。君は彼方に、不安と疑念に揺れる", weight: 1}, // 魔法使いの夜
        {text: "すべてを胸にしまい、夜明けに旅立つ", weight: 1}, // 魔法使いの夜
        {text: "星々が輝き、堕ちたこの地でも光は消えない", weight: 1}, // 魔法使いの夜
        {text: "その光が、君の心を照らす", weight: 1}, // 魔法使いの夜
        {text: "どの世界線にいても、君はひとりじゃない", weight: 1}, // シュタインズ・ゲート
        {text: "どの世界線でも、必ず君を見つけ出す", weight: 1}, // シュタインズ・ゲート
        {text: "君が僕を観測していたように、僕も君を観測し続ける", weight: 1}, // シュタインズ・ゲート
        {text: "一昨日はうさぎ、昨日は鹿、そして今日は君", weight: 1}, // CLANNAD
        {text: "朝の寒さに秋の深まりを感じ、静かな時を過ごす佳人", weight: 1}, // WHITE ALBUM2
        {text: "幾度も心を砕きながら、春に雪を溶かし君に語る", weight: 1}, // WHITE ALBUM2
        {text: "澄んだ瞳と黒髪の佳人、心を奪われたのはただ春のせい", weight: 1}, // WHITE ALBUM2
        {text: "夢の中で秋の深まりに気づかず、残る想いは他の誰のためでもない", weight: 1}, // WHITE ALBUM2
        {text: "この世界、前に進めば未来、後ろを見れば思い出。一片を取れば物語", weight: 1}, // グリザイアの果実
        {text: "それは数多の物語の、ほんの氷山の一角にすぎない", weight: 1}, // グリザイアの果実
        {text: "もし宇宙の歴史が一年なら、人の命はわずか0.1秒", weight: 1}, // Lunaria
        {text: "僕たちは、その一瞬の『瞬き』の中で出会った", weight: 1}, // Lunaria
        {text: "名前のないものは、感じられても理解はできない", weight: 1}, // 青空の下のカミュ
        {text: "僕たちはまだ、悲しみを無視できるほど完璧じゃない", weight: 1}, // Harmonia
        {text: "いや、それは完璧とは言えない", weight: 1}, // Harmonia
        {text: "悲しみを感じられるからこそ、感情を持つことが許される", weight: 1}, // Harmonia
        {text: "どこにでも行けるということは、どこにも居場所がないということ", weight: 1}, // ef - a tale of memories（悠久の翼）
        {text: "行きたい場所も、帰れる場所もない……", weight: 1},// ef - a tale of memories（悠久の翼）
        {text: "楽園と墓地は同義語ですか？", weight: 1}, // ATRI
        {
            text: "宇宙に永遠なものなどない。だから物事の終わりを嘆いても意味がない。終わりを迎えるまでにどう過ごすかが重要なんだろう？",
            weight: 1
        }, // ATRI
        {text: "いつか死ぬからといって諦めるなら、人間の一生に意味なんてない。", weight: 1}, // ATRI
        {text: "もし地球が救えなくても......地球には私も含まれますか？", weight: 1}, // ATRI
        {text: "私にとって、あなたは地球の中心です。", weight: 1}, // ATRI
        {text: "時よ止まれ、お前はなんて美しいんだ——", weight: 1}, // ATRI
        {text: "過去を変えられないからこそ、人は今日を選ぶ。", weight: 1}, // Steins;Gate 0
        {text: "機械と人間の差は、夢を見ることができるかどうかだ。", weight: 1}, // 攻殻機動隊
        {text: "感情はデータではない。それは人間を人間たらしめるものだ。", weight: 1}, // 攻殻機動隊
        {text: "世界は一つの実験場だ。観察者は常に自分自身である。", weight: 1}, // Steins;Gate
        {text: "未来は選択の積み重ね。どこにも定まってはいない。", weight: 1}, // Steins;Gate
        {text: "真実は信じる者だけに見える幻かもしれない。", weight: 1}, // PSYCHO-PASS
        {text: "意識とは、自己を実現するためのコードである。", weight: 1}, // Serial Experiments Lain
        {text: "時間はただの概念。記憶があれば永遠は存在する。", weight: 1}, // Steins;Gate 0
        {text: "もし神がいるとしたら、それは人間の意志だ。", weight: 1}, // Eden of the East
        {text: "シミュレーションか現実か。それを判断するのは感情だ。", weight: 1}, // パプリカ
        {text: "人は誰しも、自分の可能性を恐れている。", weight: 1}, // PSYCHO-PASS
    ];

    function removeHistoryItem(index: number) {
        searchHistory.update(history => {
            const newHistory = [...history];
            newHistory.splice(index, 1);
            return newHistory;
        });
    }

    const searchHistory = writable<string[]>([]);
    const maxHistoryItems = 10;

    function clearAllHistory() {
        searchHistory.set([]);
    }

    function toggleResultVisibility() {
        const hasQuery = $query.length > 0;
        const hasResults = get(filteredModules).length > 0;

        if (hasQuery) {
            addToHistory($query);
        }

        if (hasQuery && hasResults) {
            forcedShow = !forcedShow;
            showResults.set(true);
            showHistory = false;
        } else if (!hasQuery && !hasResults) {
            showResults.set(false);
            showHistory = false;
        } else {
            showHistory = !showHistory;
            showResults.set(false);
        }

        searchInputElement.focus();
    }

    function selectFromHistory(historyItem: string) {
        $query = historyItem;
        filterModules();
        showHistory = false;
        showResults.set(true);
        searchInputElement.focus();
    }

    function handleEnterKey() {
        if ($query.length > 0) {
            addToHistory($query);
            showResults.set(true);
            showHistory = false;
        } else {
            showHistory = !showHistory;
            showResults.set(false);
        }
    }

    function addToHistory(searchTerm: string) {
        if (!searchTerm.trim()) return;

        searchHistory.update(history => {
            const newHistory = history.filter(item => item !== searchTerm);
            newHistory.unshift(searchTerm);
            return newHistory.slice(0, maxHistoryItems);
        });
    }

    function filterModules() {
        hasContent = $query.length > 0;
        const pureQuery = $query.toLowerCase();

        localFiltered = hasContent
            ? modules.filter(m =>
                m.name.toLowerCase().includes(pureQuery) ||
                m.aliases.some(a => a.toLowerCase().includes(pureQuery)))
            : [];

        $filteredModules = localFiltered;

        if (hasContent) {
            showHistory = false;
        }
    }

    function handleInput() {

        if (['0721', '1011010001', 'Ciallo~(∠・ω< )⌒★'].includes($query)) {
            $query = 'Ciallo~(∠・ω< )⌒★';
            isCialloActive = true;
            const temp = new Howl({
                src: ['audio/ciallo.ogg'],
                preload: true,
                onload: () => {
                    temp.play();
                },
            });
            return;
        }

        if (isCialloActive) {
            isCialloActive = false;
            $query = '';
        }

        filterModules();

        if ($query.length > 0) {
            showHistory = false;
        }

        placeholder = getWeightedRandomPlaceholder();
    }

    let lastArrowPressTime = 0;
    const allowedInputs = ['Ciallo~(∠・ω< )⌒★'];

    async function handleKeyDown(e: KeyboardKeyEvent) {
        const validatedQuery = $query.replace(/[^a-z0-9]/gi, '');
        if (validatedQuery !== $query && !allowedInputs.includes($query)) {
            $query = validatedQuery;
            return;
        }
        if (
            e.screen === undefined ||
            !e.screen.class.startsWith("net.ccbluex.liquidbounce") ||
            !(e.screen.title === "ClickGUI" || e.screen.title === "VS-CLICKGUI")
        ) {
            return;
        }

        if (e.key === "key.keyboard.enter" && $query.length > 0 && selectedIndex === -1) {
            handleEnterKey();
        }

        if ($filteredModules.length === 0 || e.action === 0) {
            return;
        }

        const now = Date.now();
        const isArrowKey = e.key === "key.keyboard.down" || e.key === "key.keyboard.up";

        if (isArrowKey && now - lastArrowPressTime < 100) {
            return; // 小于100ms时忽略
        }

        if (isArrowKey) {
            lastArrowPressTime = now;
        }

        switch (e.key) {
            case "key.keyboard.down":
                selectedIndex = (selectedIndex + 1) % localFiltered.length;
                break;
            case "key.keyboard.up":
                selectedIndex = (selectedIndex - 1 + localFiltered.length) % localFiltered.length;
                break;
            case "key.keyboard.enter":
                if (selectedIndex >= 0) {
                    await toggleModule(localFiltered[selectedIndex].name, !localFiltered[selectedIndex].enabled);
                }
                break;
            case "key.keyboard.tab":
                const m = localFiltered[selectedIndex]?.name;
                if (m) $highlightModuleName = m;
                break;
        }

        resultElements[selectedIndex]?.scrollIntoView({
            behavior: "smooth",
            block: "nearest",
        });
    }

    function handleWindowClick(e: MouseEvent) {
        if (!searchContainerElement) return;
        const clickedInside = searchContainerElement.contains(e.target as Node);

        if (clickedInside) {
            clickOutsideCount = 0;
            return;
        }

        const now = Date.now();
        if (now - lastClickTime < 300) {
            clickOutsideCount++;
        } else {
            clickOutsideCount = 1;
        }
        lastClickTime = now;

        if (clickOutsideCount >= 2) {
            showResults.set(false);
            forcedShow = false;
            showHistory = false;
            clickOutsideCount = 0;
        } else if (!hasContent) {
            showResults.set(false);
            showHistory = false;
        }
    }


    function handleWindowKeyDown() {
        if (document.activeElement !== document.body) {
            return;
        }

        if (autoFocus) {
            searchInputElement.focus();
        }
    }

    function handleBrowserKeyDown(e: KeyboardEvent) {

        const allowedKeys = /^[a-z0-9]$|^Arrow(Down|Up|Left|Right)$|^Tab$|^Enter$|^Backspace$|^Delete$/i;


        if (!allowedKeys.test(e.key)) {
            e.preventDefault();
            return;
        }
        if (e.key === "Enter") {
            handleEnterKey();
            e.preventDefault();
        }

        if (e.key === "ArrowDown" || e.key === "ArrowUp" || e.key === "Tab") {
            e.preventDefault();
        }
    }

    async function toggleModule(name: string, enabled: boolean) {
        await setModuleEnabled(name, enabled);
    }

    function applyValues(configurable: ConfigurableSetting) {
        autoFocus = configurable.value.find(v => v.name === "SearchBarAutoFocus")?.value as boolean ?? true;
    }

    function getWeightedRandomPlaceholder(): string {
        const totalWeight = options.reduce((sum, opt) => sum + opt.weight, 0);
        let tries = 10;

        while (tries--) {
            let random = Math.random() * totalWeight;

            for (const opt of options) {
                if (random < opt.weight) {
                    if (!recentPlaceholders.includes(opt.text)) {
                        recentPlaceholders.unshift(opt.text);
                        if (recentPlaceholders.length > maxRecent) {
                            recentPlaceholders.pop();
                        }
                        return opt.text;
                    }
                    break;
                }
                random -= opt.weight;
            }
        }


        const fallback = options[Math.floor(Math.random() * options.length)].text;
        recentPlaceholders.unshift(fallback);
        if (recentPlaceholders.length > maxRecent) recentPlaceholders.pop();
        return fallback;
    }

    onMount(() => {
        const fetchSettings = async () => {
            const clickGuiSettings = await getModuleSettings("ClickGUI");
            applyValues(clickGuiSettings);
            placeholder = getWeightedRandomPlaceholder();
            if (autoFocus) {
                searchInputElement.focus();
            }
        };
        fetchSettings();
        interval = setInterval(() => {
            if (searchInputElement?.matches && !searchInputElement.matches(':focus')) {
                placeholder = getWeightedRandomPlaceholder();
            }
        }, 5000);
        return () => clearInterval(interval);
    });

    onDestroy(() => {
        clearInterval(interval);
    });
    listen("moduleToggle", (e: ModuleToggleEvent) => {
        const mod = modules.find((m) => m.name === e.moduleName);
        if (mod) {
            mod.enabled = e.enabled;
            localFiltered = $filteredModules;
        }
    });

    listen("keyboardKey", handleKeyDown);

    listen("clickGuiValueChange", (e: ClickGuiValueChangeEvent) => {
        applyValues(e.configurable);
    });

</script>

<svelte:window on:click={handleWindowClick} on:contextmenu={handleWindowClick} on:keydown={handleWindowKeyDown}/>
{#if $showSearch}
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
            class="search"
            class:has-results={$showResults || showHistory}
            class:showing={isSearchFocused || $showResults || showHistory}
            on:mouseenter={() => isSearchFocused = true}
            on:mouseleave={() => isSearchFocused = false}
            bind:this={searchContainerElement}
            in:fly|global={{y: -30, duration: 200, easing: quintOut}}
            out:fly|global={{y: -30, duration: 200, easing: quintOut}}

    >
        <div class="input-wrapper" draggable="false"
             class:focus-visible={isSearchFocused}>

            <!-- svelte-ignore element_invalid_self_closing_tag -->
            <button
                    class="search-icon"
                    class:active={$showResults}
                    class:glow={isSearchFocused}
                    aria-label="Search"
                    on:click={toggleResultVisibility}
            />
            <input
                    class="search-input"
                    bind:value={$query}
                    bind:this={searchInputElement}
                    on:focus={() => isSearchFocused = true}
                    on:focus={handleFocus}
                    on:blur={() => isSearchFocused = false}
                    on:input={filterModules}
                    on:input={handleInput}
                    on:keydown={handleBrowserKeyDown}
                    placeholder={placeholder}
                    spellcheck="false"
            />

        </div>

        <!-- 搜索结果 -->
        {#if $showResults}
            <div class="results" in:fly={{ y: 16, duration: 100, easing: quintOut }} out:shrinkOut>
                {#each localFiltered as {name, enabled, aliases}, index (name)}
                    <div
                            class="result"
                            class:enabled
                            class:selected={selectedIndex === index}
                            class:first={index === 0}
                            class:last={index === localFiltered.length - 1}
                            role="button"
                            tabindex="0"
                            in:slide={{ duration: 300, easing: quintOut }}
                            out:slide={{ duration: 200 ,easing: quintOut }}
                            on:click={() => toggleModule(name, !enabled)}
                            on:keydown={(e) => {if (e.key === 'Enter' || e.key === ' ') {e.preventDefault(); toggleModule(name, !enabled);  }}}
                            on:contextmenu|preventDefault={() => $highlightModuleName = name}
                            bind:this={resultElements[index]}
                    >
                        <div class="module-name">
                            {$spaceSeperatedNames ? convertToSpacedString(name) : name}
                        </div>
                        {#if aliases.length > 0}
                            <div class="aliases">
                                (aka {aliases.map(a => $spaceSeperatedNames ? convertToSpacedString(a) : a).join(", ")})
                            </div>
                        {/if}
                    </div>
                {/each}
            </div>

            <!-- 历史记录 -->
        {:else if showHistory}
            <div class="history-results" in:fly={{ y: 16, duration: 100, easing: quintOut }} out:shrinkOut>
                <div class="history-header">
                    <div class="title">
                        <span>Recent Searches</span>
                    </div>
                    {#if $searchHistory.length > 0}
                        <button class="clear-all" on:click|stopPropagation={clearAllHistory}>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                                <path d="M19 6L6 19M6 6l13 13" stroke="currentColor" stroke-width="2"
                                      stroke-linecap="round"/>
                            </svg>
                            <span>全部清除</span>
                        </button>
                    {/if}
                </div>
                {#each $searchHistory as historyItem, index (index)}
                    <div
                            class="history-item"
                            class:first={index === 0}
                            class:last={index === $searchHistory.length - 1}
                            role="button"
                            tabindex="0"
                            on:click={() => selectFromHistory(historyItem)}
                            on:keydown={(e) => {if (e.key === 'Enter' || e.key === ' ') {e.preventDefault(); selectFromHistory(historyItem); }}}
                    >
                        <button
                                class="remove-item"
                                on:click|stopPropagation={() => removeHistoryItem(index)}
                                aria-label="Remove this search history"
                        >
                            ×
                        </button>
                        <span class="history-text">{historyItem}</span>
                    </div>
                {/each}
            </div>
        {/if}
    </div>
{/if}
<style lang="scss">
  @use "../../colors.scss" as *;

  .search {
    position: fixed;
    top: 80px;
    left: 50%;
    transform: translateX(-50%);
    width: 600px;
    z-index: 99999999;
  }

  .input-wrapper {
    display: flex;
    align-items: center;
    overflow: hidden;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 28px;
    padding: 10px 24px;
    transition: all 0.3s cubic-bezier(0.1, 0.9, 0.2, 1);
    border: 1px solid transparent;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15),
    inset 0 0 0 1px rgba(255, 255, 255, 0.05);
    transform: translateZ(0);
    isolation: isolate;
    will-change: transform;

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, transparent 30%);
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.25s, pointer-events 0.25s;
      z-index: -1;
    }

    &:hover {
      &::before {
        opacity: 0.6;
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, transparent 50%);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15), inset 0 0 0 1px rgba(255, 255, 255, 0.05);
      }
    }

    &:focus-within {
      &::before {
        opacity: 1;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15), inset 0 0 0 1px rgba(255, 255, 255, 0.05);
        background: linear-gradient(135deg, color-mix(in srgb, var(--primary-color) 12%, transparent) 0%, transparent 70%);
        transition: opacity 0.3s ease-out, background 0.4s ease-out;
      }
    }


    &::placeholder {
      color: rgba($text, 0.5);
      transform: translateX(2px);
      opacity: 0.7;
      transition: transform 0.3s cubic-bezier(0.1, 0.9, 0.2, 1), opacity 0.2s linear;
    }


    &:active {
      transform: scale(0.98);
      transition-duration: 0.1s;
    }
  }

  .search-icon {
    width: 20px;
    height: 20px;
    background-color: rgba($text, 0.5);
    mask-image: url("/img/clickgui/icon-search.svg");
    mask-size: contain;
    mask-position: center;
    mask-repeat: no-repeat;
    transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
    border: none;
    cursor: pointer;
    margin-right: 10px;
    position: relative;
    padding: 8px;
    transform-origin: center;


    &::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%) scale(0.8);
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg,
              color-mix(in srgb, var(--primary-color) 20%, transparent) 0%,
              color-mix(in srgb, var(--secondary-color) 10%, transparent) 100%);
      border-radius: 50%;
      opacity: 0;
      transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
      z-index: -1;
    }

    &:hover {
      filter: drop-shadow(0 0 6px color-mix(in srgb, var(--primary-color) 50%, transparent));
      background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);

      &::before {
        opacity: 0.6;
        transform: translate(-50%, -50%) scale(1);
      }
    }

    &.active {
      background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
      animation: searchPulse 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;

      &::before {
        animation: circleExpand 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
      }
    }


    &.glow {
      filter: drop-shadow(0 0 12px color-mix(in srgb, var(--primary-color) 80%, transparent));

      &::before {
        opacity: 0.8;
      }
    }
  }

  .search-input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    color: $text;
    font-size: 20px;
    letter-spacing: 1px;
    font-family: "Inter", "Genshin", sans-serif;
    font-feature-settings: "frac" 0;

    &::placeholder {
      color: rgba($text, 0.5);
    }
  }

  .search-input::selection {
    background: rgba(255, 255, 255, 0.01);
    color: inherit;
    text-shadow: 0 0 4px $text;

  }


  .results, .history-results {
    max-height: 295px;
    margin-top: 8px;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 0 8px 0 0;
    border-radius: 24px;
    box-shadow: 0 16px 32px rgba(0, 0, 0, 0.4),
    inset 0 0 0 1px rgba(255, 255, 255, 0.05);
    background: linear-gradient(
                    to bottom,
                    rgba(#1a1a1a, 0.8) 0%,
                    rgba(#0d0d0d, 0.85) 100%
    );
    backdrop-filter: blur(12px);

    &::before {
      content: '';
      position: absolute;
      top: -1px;
      left: 0;
      right: 0;
      height: 2px;
      background: linear-gradient(
                      to right,
                      transparent 10%,
                      rgba($accent, 0.3) 50%,
                      transparent 90%
      );
    }


    &::-webkit-scrollbar {
      display: none;
    }
  }

  .history-results {
    .history-header {
      position: relative;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 12px 20px;


      &::before {
        content: '';
        display: inline-block;
        width: 32px;
        height: 32px;
        margin-right: 10px;
        background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
        mask-image: url("/img/clickgui/icon-history.svg");
        mask-size: contain;
        transition: transform 0.3s ease;
      }

      &::after {
        content: '';
        display: block;
        position: absolute;
        bottom: -1px;
        left: 50%;
        transform: translateX(-50%);
        width: 40%;
        height: 2px;
        background: linear-gradient(to right,
                transparent,
                color-mix(in srgb, var(--primary-color) 60%, transparent),
                transparent);

        transition: all 0.3s ease;
      }

      &:hover {
        color: var(--primary-color);

        &::before {
          transform: rotate(-10deg) scale(1.1);
          background-color: var(--primary-color);
        }

        &::after {
          width: 60%;
          background: color-mix(in srgb, var(--primary-color) 50%, transparent);
        }
      }
    }

    .title {
      font-size: var(--font-size);
      color: color-mix(in srgb, var(--primary-color) 90%, transparent);
      border-bottom: 1px solid color-mix(in srgb, var(--primary-color) 15%, transparent);
      text-transform: uppercase;
      letter-spacing: 1px;
      font-weight: 600;
      background-clip: text;
      background: linear-gradient(to right, var(--primary-color) 0%, var(--secondary-color) 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      text-shadow: 0 0 1px rgba($accent, 0.3);
    }

    .clear-all {
      position: absolute;
      right: 16px;
      top: 50%;
      transform: translateY(-50%);
      padding: 4px 10px;
      margin-left: 12px;
      font-size: var(--font-size);
      font-weight: 500;
      color: transparent;
      background: transparent;
      border: none;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;

      svg {
        width: 14px;
        height: 14px;
        display: block;
        transition: transform 0.3s ease;
      }

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: linear-gradient(135deg, rgba($red, 0.1) 0%, transparent 100%);
        opacity: 0;
        transition: opacity 0.3s ease;
      }

      &:hover {
        color: $red;
        background: rgba($red, 0.08);

        svg {
          transform: rotate(90deg);
        }

        &::before {
          opacity: 1;
        }
      }

      &:active {
        transform: scale(0.95);
        background: rgba($red, 0.15);
      }

      &:focus-visible {
        outline: none;
        box-shadow: 0 0 0 2px rgba($red, 0.3);
      }
    }

    .history-item {
      padding: 10px 20px;
      position: relative;
      transition: all 0.2s ease;
      cursor: pointer;
      color: $subtext0;
      font-size: calc(var(--font-size) + 1px);
      display: flex;
      align-items: center;

      &:not(:last-child) {
        border-bottom: 1px solid rgba($subtext0, 0.1);
      }

      &.first {
        border-top-left-radius: 0;
        border-top-right-radius: 0;
      }

      &.last {
        border-bottom-left-radius: 0;
        border-bottom-right-radius: 0;
      }


      &:hover {
        background: linear-gradient(
                        to right,
                        color-mix(in srgb, var(--primary-color) 8%, transparent) 0%,
                        color-mix(in srgb, var(--secondary-color) 4%, transparent) 100%
        );
        padding-left: 28px;
        color: $text;

        &::before {
          background-color: var(--primary-color);
          transform: scale(1.1);
        }

        .remove-item {
          opacity: 1;
        }
      }

      &:active {
        background: rgba($accent, 0.15);
      }
    }

    .remove-item {
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: transparent;
      border: none;
      color: rgba($subtext0, 0.5);
      font-size: calc(var(--font-size) + 2px);
      cursor: pointer;
      margin-right: 8px;
      border-radius: 50%;
      transition: all 0.2s ease;
      opacity: 0;

      &:hover {
        color: $red;
        background: rgba($red, 0.1);
      }
    }

    .history-text {
      flex: 1;
    }
  }

  .result {
    font-size: calc(var(--font-size) + 2px);
    padding: 14px 34px;
    position: relative;
    transition: all 0.2s ease;
    cursor: pointer;
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    align-items: center;

    &.first {
      border-top-left-radius: 0;
      border-top-right-radius: 0;
    }

    &.last {
      border-bottom-left-radius: 0;
      border-bottom-right-radius: 0;
    }

    &:not(:last-child) {
      border-bottom: 1px solid rgba($subtext0, 0.1);
    }

    .module-name {
      color: $subtext0;
      transition: all 0.2s ease;
      font-weight: 500;
    }

    &.enabled {
      .module-name {
        background-clip: text;
        background: linear-gradient(to right, var(--primary-color) 0%, var(--secondary-color) 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        text-shadow: 0 0 8px color-mix(in srgb, var(--secondary-color) 30%, transparent);
      }

      .aliases {
        color: rgba($subtext0, 0.6);
      }
    }

    .aliases {
      color: rgba($subtext0, 0.5);
      margin-left: 10px;
      font-size: var(--font-size);
      transition: color 0.2s ease;
    }

    &.selected,
    &:hover {
      background: rgba($accent, 0.08);
      padding-left: 40px;
      color: $text;
    }

    &:hover::after {
      content: "Right-click to locate";
      color: rgba($text, 0.4);
      font-size: var(--font-size);
      position: absolute;
      right: 28px;
      top: 50%;
      transform: translateY(-50%);
      padding: 4px 8px;
    }
  }

  @keyframes searchPulse {
    0% {
      transform: scale(1);
      filter: drop-shadow(0 0 4px color-mix(in srgb, var(--primary-color) 30%, transparent));
    }
    50% {
      transform: scale(1.15);
      filter: drop-shadow(0 0 16px color-mix(in srgb, var(--primary-color) 60%, transparent));
    }
    100% {
      transform: scale(1.05);
      filter: drop-shadow(0 0 8px color-mix(in srgb, var(--primary-color) 40%, transparent));
    }
  }

  @keyframes circleExpand {
    0% {
      opacity: 0;
      transform: translate(-50%, -50%) scale(0.8);
    }
    50% {
      opacity: 0.8;
      transform: translate(-50%, -50%) scale(1.2);
    }
    100% {
      opacity: 0.6;
      transform: translate(-50%, -50%) scale(1);
    }
  }
</style>
