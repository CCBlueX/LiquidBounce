import * as SliderPrimitive from "@radix-ui/react-slider";
import { useState } from "react";

import { NumberModuleSetting, RangeModuleSetting } from "../../use-modules";
import ModuleSetting from "./setting";

import styles from "./setting.module.scss";

type SliderModuleSettingProps = {
  setting: NumberModuleSetting | RangeModuleSetting;
};

export default function SliderModuleSetting({
  setting,
}: SliderModuleSettingProps) {
  const [value, setValue] = useState<number[]>(
    Array.isArray(setting.value) ? setting.value : [setting.value]
  );
  const [isGrabbing, setIsGrabbing] = useState(false);

  function handlePointerDown() {
    setIsGrabbing(true);
  }

  function handlePointerUp() {
    setIsGrabbing(false);
  }

  return (
    <ModuleSetting data-type={setting.type}>
      <div className={styles.header}>
        <div className={styles.label}>{setting.name}</div>
        <div className={styles.value}>
          {Array.isArray(value) ? value.join(" - ") : value}
        </div>
      </div>

      <SliderPrimitive.Root
        className={styles.slider}
        value={value}
        min={setting.min}
        max={setting.max}
        step={setting.step}
        onValueChange={setValue}
        data-grabbing={isGrabbing}
      >
        <SliderPrimitive.Track className={styles.track}>
          <SliderPrimitive.Range className={styles.range} />
        </SliderPrimitive.Track>
        {setting.type === "range" ? (
          <SliderPrimitive.Thumb
            className={styles.thumb}
            onPointerDown={handlePointerDown}
            onPointerUp={handlePointerUp}
          />
        ) : null}
        <SliderPrimitive.Thumb
          className={styles.thumb}
          onPointerDown={handlePointerDown}
          onPointerUp={handlePointerUp}
        />
      </SliderPrimitive.Root>
    </ModuleSetting>
  );
}
