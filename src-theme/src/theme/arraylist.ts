import {getModuleSettings} from "../integration/rest";
import type {ChoiceSetting, ChooseSetting, ConfigurableSetting,} from "../integration/types";

export async function getPrefixAsync(name: string) {
    const settings = await getModuleSettings(name);
    let value = "";
    let mode = settings.value.find(n => n.valueType == "CHOICE");
    if (mode != null) {
        const cMode = mode as ChoiceSetting;
        value = " " + cMode.active;
    } else {
        mode = settings.value.find(n => n.valueType == "CONFIGURABLE");
        if (mode != null) {
            const cMode = mode as ConfigurableSetting;
            const mode1 = cMode.value.find(n => n.valueType == "CHOICE");
            const mode2 = cMode.value.find(n => n.valueType == "CHOOSE");
            if (mode1 != null) {
                const cMode1 = mode1 as ChoiceSetting;
                value = " " + cMode1.active;
            } else {
                if (mode2 != null) {
                    const cMode1 = mode2 as ChooseSetting;
                    value = " " + cMode1.value;
                }
            }
        }
    }

    if (value == undefined) value = "";
    return Promise.resolve(value);
}