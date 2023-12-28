import { useRef, useState } from "react";
import { ColorModuleSetting } from "../../use-modules";
import ModuleSetting from "./setting";

import styles from "./setting.module.scss";

type ColorModuleSettingProps = {
  setting: ColorModuleSetting;
};

function isColorDark(hexColor: string) {
  const r = parseInt(hexColor.slice(1, 3), 16);
  const g = parseInt(hexColor.slice(3, 5), 16);
  const b = parseInt(hexColor.slice(5, 7), 16);

  const luminance = 0.299 * r + 0.587 * g + 0.114 * b;

  return luminance < 128;
}

export default function ColorModuleSetting({
  setting,
}: ColorModuleSettingProps) {
  const [value, setValue] = useState(setting.value);

  const inputRef = useRef<HTMLInputElement>(null);

  function handlePointerDown() {
    inputRef.current?.click();
  }

  return (
    <ModuleSetting data-type={setting.type}>
      <div className={styles.container} onPointerDown={handlePointerDown}>
        <div
          data-dark={isColorDark(value)}
          className={styles.color}
          style={{ backgroundColor: value }}
        />
        <input
          ref={inputRef}
          type="color"
          className={styles.input}
          value={value}
          onChange={(e) => setValue(e.target.value)}
        />
        <div className={styles.label}>{setting.name}: </div>
        <input
          type="text"
          className={styles.value}
          value={value}
          onPointerDown={(e) => e.stopPropagation()}
          onChange={(e) => {
            // Only allow valid hex color codes
            if (/^#[0-9a-f]{0,6}$/i.test(e.target.value)) {
              setValue(e.target.value);
            }
          }}
        />
      </div>
    </ModuleSetting>
  );
}
