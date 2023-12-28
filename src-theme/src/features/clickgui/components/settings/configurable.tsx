import * as Collapsible from "@radix-ui/react-collapsible";
import { motion } from "framer-motion";
import { useState } from "react";

import ModuleSetting from "./setting";

import { Values, type ConfigurableValue, type Module, ToggleableValue } from "~/utils/api";

import { ReactComponent as Chevron } from "~/assets/icons/chevron.svg";

import styles from "./settings.module.scss";

type ModuleConfigurableSettingProps = {
  setting: ConfigurableValue | ToggleableValue;
  module: Module;
  renderModuleSetting: (value: Values) => React.ReactNode;
};

export default function ModuleConfigurableSetting({
  setting,
  module,
  renderModuleSetting,
}: ModuleConfigurableSettingProps) {
  const [expanded, setExpanded] = useState(() => {
    const expanded = localStorage.getItem(
      `clickgui.module.${module.name}.setting.${setting.name}.expanded`
    );
    return expanded === "true";
  });

  function toggleExpanded() {
    setExpanded(!expanded);
    localStorage.setItem(
      `clickgui.module.${module.name}.setting.${setting.name}.expanded`,
      `${!expanded}`
    );
  }

  return (
    <ModuleSetting data-type="configurable">
      <Collapsible.Root
        className={styles.root}
        open={expanded}
        onOpenChange={toggleExpanded}
      >
        <Collapsible.Trigger className={styles.trigger}>
          <Chevron className={styles.chevron} />
          <span className={styles.label}>{setting.name}</span>
        </Collapsible.Trigger>
        <Collapsible.Content className={styles.content} asChild>
          <motion.div
            variants={{
              hidden: {
                opacity: 0,
                height: 0,
              },
              visible: {
                opacity: 1,
                height: "auto",
              },
            }}
            transition={{
              bounce: 0,
              ease: "easeOut",
            }}
          >
            {setting.value.map(renderModuleSetting)}
          </motion.div>
        </Collapsible.Content>
      </Collapsible.Root>
    </ModuleSetting>
  );
}
