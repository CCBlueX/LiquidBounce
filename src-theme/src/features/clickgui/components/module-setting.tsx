import { motion } from "framer-motion";
import { useQuery } from "react-query";

import { Module, Value, Values, getModuleSettings } from "~/utils/api";

import BooleanModuleSetting from "./settings/boolean-setting";
import ColorModuleSetting from "./settings/color-setting";
import EnumModuleSetting from "./settings/enum-setting";
import SliderModuleSetting from "./settings/slider-setting";
import StringModuleSetting from "./settings/string-setting";
import UnknownModuleSetting from "./settings/unknown-setting";

import styles from "./module.module.scss";

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

  console.log(settings);

  function renderModuleSetting(value: Values) {
    let component;

    switch (value.valueType) {
      case "BOOLEAN":
        component = <BooleanModuleSetting value={value} key={value.name} />;
        break;
      // case "INT":
      // case "INT_RANGE":
      // case "FLOAT":
      // case "FLOAT_RANGE":
      //   component = <SliderModuleSetting setting={value} key={value.name} />;
      //   break;
      // case "CHOICE":
      //   component = <EnumModuleSetting setting={value} key={value.name} />;
      //   break;
      // case "TEXT":
      //   component = <StringModuleSetting setting={value} key={value.name} />;
      //   break;
      // case "COLOR":
      //   component = <ColorModuleSetting setting={value} key={value.name} />;
      //   break;
      default:
        component = <UnknownModuleSetting value={value} />;
        break;
    }

    return component;
  }

  if (isFetching) return null;

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
      {settings?.value.map(renderModuleSetting)}
    </motion.div>
  );
}
