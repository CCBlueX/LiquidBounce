import styles from "./header.module.css";

type HeaderProps = {
  children?: React.ReactNode;
};

export default function Header({ children }: HeaderProps) {
  return <div className={styles.header}>{children}</div>;
}
