<!--
  - This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
  -
  - Copyright (c) 2015 - 2025 CCBlueX
  -
  - LiquidBounce is free software: you can redistribute it and/or modify
  - it under the terms of the GNU General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - LiquidBounce is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  - GNU General Public License for more details.
  -
  - You should have received a copy of the GNU General Public License
  - along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
  -->

<script lang="ts">
    import {setModuleEnabled} from "../../../integration/rest";
    import ButtonSetting from "./setting/ButtonSetting.svelte";
    import {listen} from "../../../integration/ws";
    import type {ModuleToggleEvent} from "../../../integration/events";


    interface FixedModule {
        name: string;
        enabled: boolean;
    }

    let fixedModules: FixedModule[] = [
        {name: "KillAura", enabled: false},
        {name: "ChestStealer", enabled: false},
        {name: "AutoArmor", enabled: false},
        {name: "InventoryCleaner", enabled: false}
    ].sort((a, b) => b.name.length - a.name.length);


    async function toggleModule(mod: FixedModule) {
        await setModuleEnabled(mod.name, !mod.enabled);
        mod.enabled = !mod.enabled;
        fixedModules = [...fixedModules];
    }

    listen("moduleToggle", (e: ModuleToggleEvent) => {
        const mod = fixedModules.find(m => m.name === e.moduleName);
        if (!mod) return;
        mod.enabled = e.enabled;
        fixedModules = [...fixedModules];
    });
</script>

<div class="buttons">
    {#each fixedModules as mod}
        <ButtonSetting
                title={mod.enabled ? mod.name : ` ${mod.name}`}
                on:click={() => toggleModule(mod)}
                secondary={mod.enabled}
                matchWidth={true}
                centerText={true}
        />
    {/each}
</div>

<style lang="scss">
  .buttons {
    display: flex;
    transform: scale(0.8);
    gap: 10px;
    position: absolute;
    flex-direction: column;
    align-items: flex-start;
    top: 20px;
    left: 20px;
  }
</style>
