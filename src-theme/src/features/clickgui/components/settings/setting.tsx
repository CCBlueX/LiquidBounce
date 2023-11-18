import { motion } from "framer-motion";

import { ModuleSetting } from "../../use-modules";

import styles from "./setting.module.scss";

export type ModuleSettingProps = {
  setting: ModuleSetting;
};

type ModuleSettingItemProps = {
  children: React.ReactNode;
  className?: string;
};

export default function ModuleSetting(props: ModuleSettingItemProps) {
  return (
    <motion.div
      variants={{
        hidden: {
          opacity: 0,
          x: "-100%",
        },
        visible: {
          opacity: 1,
          x: 0,
        },
      }}
      className={styles.setting}
      transition={{ ease: "anticipate", duration: 0.4 }}
      {...props}
    />
  );
}
