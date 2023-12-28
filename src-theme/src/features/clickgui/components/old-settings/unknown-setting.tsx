import ModuleSetting, { ModuleSettingProps } from "./setting";

import styles from "./setting.module.scss";

export default function UnknownModuleSetting({ value }: ModuleSettingProps) {
  return (
    <ModuleSetting data-type="unknown">
      <div className={styles.label}>
        Unknown setting type "{value.valueType}"
      </div>
    </ModuleSetting>
  );
}
