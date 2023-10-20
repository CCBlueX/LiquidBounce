import { useState } from "react";

import Switch from "~/components/Switch";

import SearchBar from "./SearchBar";

type HeaderProps = {
  onSearch: (value: string) => void;
};

export default function Header({ onSearch }: HeaderProps) {
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [currentVersion, setCurrentVersion] = useState(false);
  const [online, setOnline] = useState(false);

  return (
    <div className="h-[92px] bg-black/70 rounded-xl flex items-center space-x-10 px-8">
      {/* Search Bar */}
      <SearchBar onChange={onSearch} />
      <Switch value={favoritesOnly} onChange={setFavoritesOnly}>
        Favorites Only
      </Switch>
      <Switch value={currentVersion} onChange={setCurrentVersion}>
        Current Version
      </Switch>
      <Switch value={online} onChange={setOnline}>
        Online
      </Switch>
    </div>
  );
}
