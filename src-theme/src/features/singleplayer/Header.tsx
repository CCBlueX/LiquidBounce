import { useState } from "react";

import Switch from "~/components/Switch";

import SearchBar from "./SearchBar";
import Combobox, { Option } from "~/components/Combobox";

type HeaderProps = {
  onSearch: (value: string) => void;
};

export default function Header({ onSearch }: HeaderProps) {
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [gameModes, setGameModes] = useState<Option[]>([
    {
      label: "Survival",
      value: "survival",
    },
    {
      label: "Creative",
      value: "creative",
    },
    {
      label: "Adventure",
      value: "adventure",
    },
    {
      label: "Spectator",
      value: "spectator",
    },
  ]);
  const [difficulties, setDifficulties] = useState<Option[]>([
    {
      label: "Easy",
      value: "easy",
    },
    {
      label: "Normal",
      value: "normal",
    },
    {
      label: "Hard",
      value: "hard",
    },
    {
      label: "Peaceful",
      value: "peaceful",
    },
  ]);
  const [worldTypes, setWorldTypes] = useState<Option[]>([
    {
      label: "Default",
      value: "default",
    },
    {
      label: "Superflat",
      value: "superflat",
    },
    {
      label: "Large Biomes",
      value: "large_biomes",
    },
  ]);

  return (
    <div className="h-[92px] bg-black/70 rounded-xl flex items-center space-x-10 px-8">
      {/* Search Bar */}
      <SearchBar onChange={onSearch} />
      <Switch value={favoritesOnly} onChange={setFavoritesOnly}>
        Favorites Only
      </Switch>
      <Combobox
        options={gameModes}
        onToggle={(option) =>
          setGameModes((prev) =>
            prev.map((item) =>
              item.value === option.value
                ? { ...item, checked: !item.checked }
                : item
            )
          )
        }
      >
        Game Mode
      </Combobox>
      <Combobox
        options={difficulties}
        onToggle={(option) =>
          setDifficulties((prev) =>
            prev.map((item) =>
              item.value === option.value
                ? { ...item, checked: !item.checked }
                : item
            )
          )
        }
      >
        Difficulty
      </Combobox>
      <Combobox
        options={worldTypes}
        onToggle={(option) =>
          setWorldTypes((prev) =>
            prev.map((item) =>
              item.value === option.value
                ? { ...item, checked: !item.checked }
                : item
            )
          )
        }
      >
        World Type
      </Combobox>
    </div>
  );
}
