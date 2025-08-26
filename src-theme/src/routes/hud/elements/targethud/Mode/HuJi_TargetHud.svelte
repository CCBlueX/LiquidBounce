<script lang="ts">
    import {listen} from "../../../../../integration/ws.js";
    import type {PlayerData} from "../../../../../integration/types";
    import {REST_BASE} from "../../../../../integration/host";
    import {calcArmorColor} from "../../../../../util/Client/calcArmorColor";
    import {detectTeamColor, type TeamColor} from "../../../../../util/Client/calcArmorColor";
    import type {TargetChangeEvent} from "../../../../../integration/events.js";
    import {popOut, popIn} from "../../../../../util/animate_utils";
    import PlayerView from "../../../common/PlayerView/PlayerView.svelte";
    import {visible} from "../TargetHud";

    interface NameHistoryEntry {
        name: string;
        changedToAt: string; // "2020/03/10 03:52:18" or ""
    }

    interface UAPIResponse {
        history: NameHistoryEntry[];
    }

    let target: PlayerData | null = null;
    let teamColor: TeamColor = null;
    let xp = "不明";

    let birthYear = "2009";
    let birthMonth = "5";
    let birthDay = "17";


    let isPremium = false;

    const xpCache = new Map<string, string>();

    function getRandomXP() {
        const xps = ["恋物", "恋童", "施虐", "受虐", "贫乳", "巨乳", "绑缚", "异瞳"];
        return xps[Math.floor(Math.random() * xps.length)];
    }

    function getXPForTarget(id: string | null) {
        if (!id) return "不明";
        if (!xpCache.has(id)) xpCache.set(id, getRandomXP());
        return xpCache.get(id)!;
    }


    function isOfflineUUID(uuid: string) {
        return !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(uuid);
    }

    function getDefaultBirthDate() {
        return new Date("2009-05-17");
    }

    function parseDateString(s: string): Date | null {
        if (!s) return null;
        const iso = s.replace(/\//g, "-").replace(" ", "T");
        const d = new Date(iso);
        return isNaN(d.getTime()) ? null : d;
    }


    const infoCache = new Map<
        string,
        Promise<{ year: string; month: string; day: string; premium: boolean }>
    >();

    function removeColorCodes(str: string): string {

        return str.replace(/§[0-9a-fA-F]/g, '');
    }

    async function fetchTargetInfo(uuid: string) {

        let date: Date;
        if (isOfflineUUID(uuid)) {
            date = getDefaultBirthDate();
        } else {
            try {
                const clean = uuid.replace(/-/g, "");
                const res = await fetch(`https://uapis.cn/api/mchistoryid?uuid=${clean}`);
                if (!res.ok) throw new Error();
                const data = (await res.json()) as UAPIResponse;

                const timestamps = data.history
                    .map((e) => parseDateString(e.changedToAt))
                    .filter((d): d is Date => d !== null)
                    .map((d) => d.getTime());
                date = timestamps.length > 0
                    ? new Date(Math.min(...timestamps))
                    : new Date("2010-06-13");
            } catch {
                date = getDefaultBirthDate();
            }
        }

        let premium = false;
        if (!isOfflineUUID(uuid)) {
            try {
                const clean = uuid.replace(/-/g, "");
                const response = await fetch(
                    `https://sessionserver.mojang.com/session/minecraft/profile/${clean}`
                );

                if (response.ok) {
                    const data = await response.json();

                    premium = Array.isArray(data.properties) &&
                        data.properties.some((prop: { name: string; value: string }) => {
                            return prop.name === "textures" &&
                                prop.value &&
                                prop.value.length > 0;
                        });
                }
            } catch (error) {
                console.error("正版验证失败:", error);
                premium = false;
            }
        }
        return {
            year: String(date.getFullYear()),
            month: String(date.getMonth() + 1),
            day: String(date.getDate()),
            premium
        };
    }

    async function updateTargetInfo(uuid: string) {

        if (!infoCache.has(uuid)) {
            infoCache.set(uuid, fetchTargetInfo(uuid));
        }

        const {year, month, day, premium} = await infoCache.get(uuid)!;
        birthYear = year;
        birthMonth = month;
        birthDay = day;
        isPremium = premium;
    }

    listen("targetChange", (data: TargetChangeEvent) => {
        target = data.target;

        if (target) {
            xp = getXPForTarget(target.username);
            teamColor = detectTeamColor(calcArmorColor(target.armorItems));

            updateTargetInfo(target.uuid);
        }
    });

</script>
<div class="main-wrapper" class:draggable={!$visible && !target}>
    {#if $visible && target }
        {#if !target.isDead}
            <div class="id-card"
                 in:popIn|global={{ duration: 400 }}
                 out:popOut|global={{ duration: 300 }}>
                <div class="content">
                    <div class="info">
                        <div class="info-row">
                            <span class="label">姓 名：</span>
                            <span class="value">{removeColorCodes(target.username)}</span>
                        </div>
                        <div class="info-row combined-row">
                            <div class="combined-item">
                                <span class="label">性 癖：</span>
                                <span class="value">{xp}</span>
                            </div>

                            <div class="combined-item">
                                <span class="label">正 版：</span>
                                <span class="value">{isPremium ? '是' : '否'}</span>
                            </div>
                        </div>

                        <div class="info-row birth-row">
                            <span class="label">出 生：</span>
                            <div class="value date">
                                <span class="part">{birthYear}</span><span class="unit">年</span>
                                <span class="part">{birthMonth}</span><span class="unit">月</span>
                                <span class="part">{birthDay}</span><span class="unit">日</span>
                            </div>
                        </div>
                        <div class="info-row">
                            <span class="label">现 居：</span>
                            <span class="value">X:{Math.round(target.position.x)} Y:{Math.round(target.position.y)}
                                Z:{Math.round(target.position.z)}</span>
                        </div>
                        {#if teamColor}
                            <div class="info-row">
                                <span class="label">队 伍：</span>
                                <span class="value">{teamColor}队</span>
                            </div>
                        {/if}
                    </div>
                    <div class="view-container">
                        <div class="view">
                            <div class="view-inner">
                                <PlayerView skinUrl={`${REST_BASE}/api/v1/client/resource/skin?uuid=${target.uuid}`}/>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="footer">
                    <div class="sfz">唯一标识：</div>
                    <div class="uuid">{target.uuid}</div>
                </div>
            </div>
        {:else}
            <div class="empty-placeholder"></div>
        {/if}
    {/if}
</div>
<style lang="scss">
  @use "../../../../../colors.scss" as *;

  .main-wrapper {
    display: flex;
    justify-content: center;
    flex-direction: column;
    align-items: flex-end;
    position: absolute;
    border: 6px dashed transparent;
    border-radius: 10px;
    min-width: 450px;
    min-height: 280px;
    transition: background-color, border-color 0.3s ease;

    &:hover {
      background: rgba(204, 204, 204, 0.2);
      border-color: #ccc;

    }

    .empty-placeholder {
      display: none;
    }

    &.draggable {
      cursor: move;

      &:hover {
        border-color: rgba(255, 255, 255, 0.8) !important;
        background: rgba(204, 204, 204, 0.3);
      }
    }
  }

  .id-card {
    width: 450px;
    min-height: 280px;
    background-image: url('/img/hud/targethud/sfz.png');
    background-size: 100% 100%;
    filter: drop-shadow(0 0 4px rgba($base, 0.5));
    background-position: center;
    border-radius: 10px;
    padding: 40px;
    color: #333;
    font-family: 'Microsoft YaHei', 'SimHei', sans-serif;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .content {
    position: relative;
    display: flex;
    align-items: flex-start;
    flex: 1;
    margin-bottom: 0;

  }

  .view-container {
    position: absolute;
    top: 0;
    right: -45px;
    width: 150px;
    height: 175px;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: none;

    .view {
      position: relative;

      .view-inner {
        position: absolute;
        top: 40%;
        left: 50%;
        transform: translate(-50%, -50%) scale(5.5);
        filter: drop-shadow(0 0 4px rgba($base, 0.5));
      }
    }
  }

  .info {
    .info-row {
      display: flex;
      align-items: center;
      font-size: 16px;
      line-height: 24px;
      margin-bottom: 8px;

      .label {
        width: 80px;
        font-weight: bold;
        color: #3C96CA;
        margin-right: 8px;
        flex-shrink: 0;
        text-align: justify;
        text-align-last: justify;
      }

      .value {
        flex: 1;
        color: #131220;
        word-break: break-all;
      }

      &.combined-row {
        display: flex;
        gap: 8px;

        .combined-item {
          display: flex;
          flex: 1;
          min-width: 0;
          align-items: center;

          .label {
            width: 80px;
          }
        }
      }

      &.birth-row {
        display: flex;
        align-items: center;

        .value.date {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          font-size: 16px;

          .part {
            min-width: 30px;
            text-align: center;
            line-height: 24px;
            color: #131220;
            margin-right: 4px;
          }

          .unit {
            margin: 0 4px 0 0;
            font-weight: bold;
            line-height: 24px;
            color: #3C96CA;
          }
        }
      }
    }
  }

  .footer {
    margin-top: auto;
    display: flex;
    align-items: center;
    font-size: 14px;
    color: #3C96CA;
    white-space: nowrap;

    .sfz {
      width: 80px;
      font-size: 16px;
      font-weight: bold;
      letter-spacing: 2px;
      margin-right: 8px;
      flex-shrink: 0;
    }

    .uuid {
      flex: 1;
      color: #131220;
      overflow: hidden;
      text-overflow: ellipsis;
      padding-right: 15px;
    }
  }
</style>
