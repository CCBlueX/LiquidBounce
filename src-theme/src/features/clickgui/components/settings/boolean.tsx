import * as SwitchPrimitive from "@radix-ui/react-switch";
import { useState } from "react";

import ModuleSetting from "./setting";

import { type BooleanValue, type Module } from "~/utils/api";

import styles from "./settings.module.scss";

type ModuleBooleanSettingProps = {
  setting: BooleanValue;
  module: Module;
};

export default function ModuleBooleanSetting({
  setting,
  module,
}: ModuleBooleanSettingProps) {
  const [checked, setChecked] = useState(setting.value);

  function onChange(checked: boolean) {
    setChecked(!checked);
    // TODO: Update setting value
  }

  return (
    <ModuleSetting data-type="boolean">
      <div className={styles.container}>
        <SwitchPrimitive.Root
          className={styles.root}
          checked={checked}
          onCheckedChange={onChange}
        >
          <SwitchPrimitive.Thumb className={styles.thumb} />
        </SwitchPrimitive.Root>
        <div className={styles.label}>{setting.name}</div>
      </div>
    </ModuleSetting>
  );
}
