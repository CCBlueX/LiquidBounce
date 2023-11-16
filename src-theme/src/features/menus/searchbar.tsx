import { ReactComponent as FilterIcon } from "~/assets/icons/filter.svg";
import { ReactComponent as SearchIcon } from "~/assets/icons/search.svg";

import styles from "./searchbar.module.css";

type SearchBarProps = {
  onChange: (value: string) => void;
};

export default function SearchBar({ onChange }: SearchBarProps) {
  return (
    <div className={styles.container}>
      <SearchIcon className={styles.searchIcon} />
      <input
        type="text"
        className={styles.input}
        placeholder="Search..."
        onChange={(e) => onChange(e.target.value)}
      />
      <FilterIcon className={styles.filterIcon} />
    </div>
  );
}
