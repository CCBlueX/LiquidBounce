import {useModules} from "~/features/clickgui/useModules.tsx";

import styles from "./clickgui.module.css";
import Panel from "~/features/clickgui/components/Panel.tsx";
import {CSSProperties} from "react";

export default function ClickGUI() {
    const {modulesByCategory} = useModules()

    // let modulesColor = kotlin.colorToHex(clickGuiModule.instance.getModuleColor())
    //     let headerColor = kotlin.colorToHex(clickGuiModule.instance.getHeaderColor())
    //     let accentColor = kotlin.colorToHex(clickGuiModule.instance.getAccentColor())
    //     let accendDimmed = kotlin.colorToHex(clickGuiModule.instance.getAccentColor())
    //     let textColor = kotlin.colorToHex(clickGuiModule.instance.getTextColor())
    //     let textDimmedColor = kotlin.colorToHex(clickGuiModule.instance.getDimmedTextColor())
    let style = {
        "--module": "#000000",
        "--header": "#000000",
        "--accent": "#000000",
        "--accent-dimmed": "#121212",
        "--text": "#fff",
        "--text-dimmed": "#aaa",
    } as CSSProperties;

    return (
        <div
            className={styles.clickgui}
            style={style}
        >
            {Object.entries(modulesByCategory).map(([category, modules], idx) => (
                <Panel category={category} modules={modules} key={category}
                       startPosition={[
                           30,
                           30 + (idx * 45),
                       ]}
                />
            ))}
        </div>
    );
}
