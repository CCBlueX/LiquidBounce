import * as SliderPrimitive from "@radix-ui/react-slider";
import { useState } from "react";

import ModuleSetting from "./setting";

import {
  type FloatValue,
  type FloatRangeValue,
  type IntValue,
  type IntRangeValue,
  type Module,
} from "~/utils/api";

import styles from "./settings.module.scss";

type NumericValue = FloatValue | FloatRangeValue | IntValue | IntRangeValue;

type ModuleNumericSettingProps = {
  setting: NumericValue;
  module: Module;
};

export default function ModuleNumericSetting({
  setting,
  module,
}: ModuleNumericSettingProps) {
  const isRange =
    setting.valueType === "INT_RANGE" || setting.valueType === "FLOAT_RANGE";

  const [value, setValue] = useState<number[]>(
    isRange ? [setting.value.from, setting.value.to] : [setting.value]
  );
  const [isGrabbing, setIsGrabbing] = useState(false);

  function handlePointerDown() {
    setIsGrabbing(true);
  }

  function handlePointerUp() {
    setIsGrabbing(false);
  }

  return (
    <ModuleSetting data-type="numeric">
      <div className={styles.header}>
        <div className={styles.label}>{setting.name}</div>
        <div className={styles.value}>
          {Array.isArray(value) ? value.join(" - ") : value}
        </div>
      </div>

      <SliderPrimitive.Root
        className={styles.slider}
        value={value}
        min={setting.range.from}
        max={setting.range.to}
        onValueChange={setValue}
        data-grabbing={isGrabbing}
      >
        <SliderPrimitive.Track className={styles.track}>
          <SliderPrimitive.Range className={styles.range} />
        </SliderPrimitive.Track>
        {isRange ? (
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
