import ModuleSetting, { ModuleSettingProps } from "./setting";

import styles from "./setting.module.scss";

export default function UnknownModuleSetting({ setting }: ModuleSettingProps) {
  return (
    <ModuleSetting data-type="unknown">
      <div className={styles.label}>Unknown setting type "{setting.type}"</div>
    </ModuleSetting>
  );
}
