import { motion } from "framer-motion";
import { useQuery } from "react-query";

import ModuleConfigurableSetting from "./configurable";
import ModuleSetting from "./setting";

import { type Module, type Values, getModuleSettings } from "~/utils/api";

import styles from "./settings.module.scss";
import ModuleBooleanSetting from "./boolean";
import ModuleNumericSetting from "./numeric";
import ModuleChooseSetting from "./choose";

type ModuleSettingsProps = {
  module: Module;
};

export default function ModuleSettings({ module }: ModuleSettingsProps) {
  const { data: settings, isFetching } = useQuery(
    `module.${module.name}.settings`,
    () => getModuleSettings(module.name),
    {
      refetchOnWindowFocus: false,
    }
  );

  function renderModuleSetting(value: Values) {
    let component;

    switch (value.valueType) {
      case "CONFIGURABLE":
      case "TOGGLEABLE": // TODO: Toggleable setting is a bit different from configurable
        component = (
          <ModuleConfigurableSetting
            key={value.name}
            module={module}
            setting={value}
            renderModuleSetting={renderModuleSetting}
          />
        );
        break;
      case "BOOLEAN":
        component = (
          <ModuleBooleanSetting
            key={value.name}
            module={module}
            setting={value}
          />
        );
        break;
      case "INT":
      case "INT_RANGE":
      case "FLOAT":
      case "FLOAT_RANGE":
        component = (
          <ModuleNumericSetting
            key={value.name}
            module={module}
            setting={value}
          />
        );
        break;
      case "CHOOSE":
        component = (
          <ModuleChooseSetting
            key={value.name}
            module={module}
            setting={value}
          />
        );
        break;
      default:
        component = (
          <ModuleSetting key={value.name} data-type={value.valueType}>
            <div className={styles.label}>
              Unknown setting type "{value.valueType}"
            </div>
          </ModuleSetting>
        );
        break;
    }

    return component;
  }

  if (isFetching) return null;

  const sortedSettings = settings?.value.sort((a, b) => {
    const aIsConfigurable =
      a.valueType === "CONFIGURABLE" || a.valueType === "TOGGLEABLE";
    const bIsConfigurable =
      b.valueType === "CONFIGURABLE" || b.valueType === "TOGGLEABLE";

    if (aIsConfigurable && !bIsConfigurable) {
      return 1;
    } else if (
      (aIsConfigurable && bIsConfigurable) ||
      (!aIsConfigurable && !bIsConfigurable)
    ) {
      return -1;
    } else {
      return 0;
    }
  });

  return (
    <motion.div
      className={styles.settings}
      variants={{
        hidden: {
          opacity: 0,
          height: 0,
        },
        visible: {
          opacity: 1,
          height: "auto",
          transition: {
            staggerChildren: 0.05,
            bounce: 0,
            ease: "easeInOut",
            duration: 0.2,
          },
        },
      }}
      initial="hidden"
      animate="visible"
      exit="hidden"
    >
      {sortedSettings?.map(renderModuleSetting)}
    </motion.div>
  );
}
