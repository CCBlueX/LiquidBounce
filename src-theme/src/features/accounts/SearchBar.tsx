import { ReactComponent as SearchIcon } from "~/assets/icons/search.svg";
import { ReactComponent as FilterIcon } from "~/assets/icons/filter.svg";

type SearchBarProps = {
  onChange: (value: string) => void;
};

export default function SearchBar({ onChange }: SearchBarProps) {
  return (
    <div className="relative h-14 rounded-md overflow-hidden flex-1">
      {/* Search Icon */}
      <SearchIcon className="absolute left-4 top-1/2 transform -translate-y-1/2 w-6 h-6 text-white pointer-events-none" />
      <input
        type="text"
        className="w-full h-full bg-black/40 px-14 text-white text-xl font-semibold border-transparent border-b-4 focus:outline-none focus:border-brand"
        placeholder="Search..."
        onChange={(e) => onChange(e.target.value)}
      />
      {/* Filter */}
      <div className="absolute right-4 top-1/2 transform -translate-y-1/2 w-6 h-6 text-white">
        <FilterIcon className="w-full h-full" />
      </div>
    </div>
  );
}
