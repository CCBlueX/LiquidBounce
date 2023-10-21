import { SVGProps } from "react";
import styles from "./button.module.css";
import Tooltip from "./Tooltip";

type ButtonProps = {
  variant?: "primary" | "ghost";
  icon?: React.ElementType<SVGProps<SVGSVGElement>>;
  children?: React.ReactNode;
  tooltip?: string;
};

export default function Button({
  variant = "primary",
  icon: Icon,
  children,
  tooltip,
}: ButtonProps) {
  const component = (
    <button className={styles.button} data-variant={variant}>
      {Icon && (
        <div className={styles.iconWrapper}>
          <Icon className={styles.icon} />
        </div>
      )}
      {children && <div className={styles.label}>{children}</div>}
    </button>
  );

  return (
    <>{tooltip ? <Tooltip text={tooltip}>{component}</Tooltip> : component}</>
  );
}
