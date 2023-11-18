import { useState } from "react";

import { StringModuleSetting } from "../../use-modules";

import ModuleSetting from "./setting";

import styles from "./setting.module.scss";

type StringModuleSettingProps = {
  setting: StringModuleSetting;
};

export default function StringModuleSetting({
  setting,
}: StringModuleSettingProps) {
  const [value, setValue] = useState(setting.value);

  return (
    <ModuleSetting data-type={setting.type}>
      <input
        type="text"
        className={styles.input}
        value={value}
        placeholder={setting.name}
        onChange={(e) => setValue(e.target.value)}
      />
    </ModuleSetting>
  );
}
