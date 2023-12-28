import { CSSProperties } from "react";
import { useQuery } from "react-query";

import AlignmentGrid, {
  AlignmentGridProvider,
} from "~/components/alignment-grid";

import Panel from "~/features/clickgui/components/panel";

import { Module, getModuleSettings, getModules } from "~/utils/api";

import styles from "./clickgui.module.css";
import { intToRgba } from "~/utils/misc";

export default function ClickGUI() {
  // const { modulesByCategory } = useModules();

  const { data: modules } = useQuery("modules", getModules, {
    refetchOnWindowFocus: false,
  });

  const modulesByCategory = modules?.reduce((acc, module) => {
    const category = module.category;

    if (!acc[category]) {
      acc[category] = [];
    }

    acc[category].push(module);
    return acc;
  }, {} as Record<string, Module[]>);

  const { data: clickGuiSettings } = useQuery(
    "clickGuiSettings",
    () => getModuleSettings("ClickGUI"),
    {
      refetchOnWindowFocus: false,
    }
  );

  if (!modulesByCategory || !clickGuiSettings) {
    return null;
  }

  function getColorSetting(name: string) {
    const setting = clickGuiSettings?.value.find(
      (setting) => setting.name === name && setting.valueType == "COLOR"
    )?.value as number;

    return intToRgba(setting);
  }

  const style = {
    "--module": getColorSetting("ModuleColor"),
    "--header": getColorSetting("HeaderColor"),
    "--accent": getColorSetting("AccentColor"),
    "--text": getColorSetting("TextColor"),
    "--text-dimmed": getColorSetting("DimmedTextColor"),
  } as CSSProperties;

  return (
    <div className={styles.clickgui} style={style}>
      <AlignmentGridProvider
        value={{
          enabled: true,
          horizontal: 16,
          vertical: 16,
        }}
      >
        {/* <AlignmentGrid /> */}
        {/* <img
          src="./background.png"
          className="absolute inset-0 w-full h-full -z-10"
        /> */}
        {modulesByCategory &&
          Object.entries(modulesByCategory).map(([category, modules], idx) => (
            <Panel
              category={category}
              modules={modules}
              key={category}
              startPosition={[30, 30 + idx * 45]}
            />
          ))}
      </AlignmentGridProvider>
    </div>
  );
}
