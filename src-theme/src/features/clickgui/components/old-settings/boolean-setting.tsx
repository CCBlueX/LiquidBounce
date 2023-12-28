import * as SwitchPrimitive from "@radix-ui/react-switch";
import { useState } from "react";

import ModuleSetting from "./setting";

import { BooleanValue } from "~/utils/api";

import styles from "./setting.module.scss";

type BooleanModuleSettingProps = {
  value: BooleanValue;
};

export default function BooleanModuleSetting({
  value,
}: BooleanModuleSettingProps) {
  const [checked, setChecked] = useState(value.value);

  function onChange(checked: boolean) {
    setChecked(!checked);
    // TODO: Update setting value
  }

  return (
    <ModuleSetting data-type={value.valueType}>
      <div className={styles.container}>
        <SwitchPrimitive.Root
          className={styles.root}
          checked={checked}
          onCheckedChange={onChange}
        >
          <SwitchPrimitive.Thumb className={styles.thumb} />
        </SwitchPrimitive.Root>
        <div className={styles.label}>{value.name}</div>
      </div>
    </ModuleSetting>
  );
}
