import {getModuleSettings} from "../integration/rest";
import type {ChoiceSetting, ChooseSetting, ConfigurableSetting} from "../integration/types";

export async function getPrefix(name: string) {
    const settings = await getModuleSettings(name);
    const prefixName = getPrefixName(settings);
    return formatPrefixName(prefixName);
}

function getPrefixName(settings: any): string | null {
    let choice = settings.value.find((n: any) => n.valueType === "CHOICE");
    if (choice != null) {
        return (choice as ChoiceSetting).active;
    }

    let configurable = settings.value.find((n: any) => n.valueType === "CONFIGURABLE");
    if (configurable == null) {
        return null;
    }

    const cMode = configurable as ConfigurableSetting;
    choice = cMode.value.find((n: any) => n.valueType === "CHOICE");
    if (choice != null) {
        return (choice as ChoiceSetting).active;
    }

    let choose = cMode.value.find((n: any) => n.valueType === "CHOOSE");
    if (choose != null) {
        return (choose as ChooseSetting).value;
    }

    return null;
}

function formatPrefixName(prefix: string | null): string {
    return prefix ? ` ${prefix}` : "";
}