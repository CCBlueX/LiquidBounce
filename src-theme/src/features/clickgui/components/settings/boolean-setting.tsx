import * as SwitchPrimitive from "@radix-ui/react-switch";
import { useState } from "react";

import { BooleanModuleSetting } from "../../use-modules";

import ModuleSetting from "./setting";

import styles from "./setting.module.scss";

type BooleanModuleSettingProps = {
  setting: BooleanModuleSetting;
};

export default function BooleanModuleSetting({
  setting,
}: BooleanModuleSettingProps) {
  const [checked, setChecked] = useState(setting.value);

  function onChange(checked: boolean) {
    setChecked(checked);
    // TODO: Update setting value
  }

  return (
    <ModuleSetting data-type={setting.type}>
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
