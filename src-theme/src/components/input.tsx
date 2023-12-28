import { ElementType, useState } from "react";

import { ReactComponent as Eye } from "~/assets/icons/eye.svg";

import styles from "./input.module.css";

type InputProps = {
  label: string;
  icon?: ElementType<React.ComponentProps<"svg">>;
  type?: string;
  children?: React.ReactNode;
};

export default function Input({
  label,
  icon: Icon,
  type = "text",
  children,
}: InputProps) {
  return (
    <div className={styles.container}>
      {Icon && (
        <div className={styles.iconWrapper}>
          <Icon className={styles.icon} />
        </div>
      )}
      <input className={styles.input} placeholder={label} type={type} />
      {children}
    </div>
  );
}

export function PasswordInput(props: InputProps) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <Input {...props} type={showPassword ? "text" : "password"}>
      <button
        type="button"
        className={styles.showPassword}
        onClick={() => setShowPassword(!showPassword)}
      >
        <Eye />
      </button>
    </Input>
  );
}
