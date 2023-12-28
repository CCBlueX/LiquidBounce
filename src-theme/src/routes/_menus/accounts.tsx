import Fuse from "fuse.js";
import { useMemo, useState } from "react";
import { useQuery } from "react-query";

import Button from "~/components/button";
import Combobox, { type Option } from "~/components/combobox";
import Switch from "~/components/switch";

import Footer from "~/features/menus/footer";
import Header from "~/features/menus/header";
import List from "~/features/menus/list";
import Menu from "~/features/menus/menu";
import SearchBar from "~/features/menus/searchbar";

import AccountEntry from "~/features/menus/accounts/account-entry";

import { Account, getAccounts } from "~/utils/api";

// Left Footer Buttons
import { ReactComponent as Clipboard } from "~/assets/icons/clipboard.svg";
import { ReactComponent as Import } from "~/assets/icons/import.svg";
import { ReactComponent as Shuffle } from "~/assets/icons/shuffle.svg";
import { ReactComponent as Check } from "~/assets/icons/check.svg";
import LoginButton from "~/features/menus/accounts/login-button";

export default function AccountManager() {
  const {
    status,
    data: accounts,
    error,
  } = useQuery<Account[], Error>("accounts", getAccounts);
  const [search, setSearch] = useState("");
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [types, setTypes] = useState<Option[]>([
    {
      label: "Microsoft",
      value: "microsoft",
    },
    {
      label: "TheAltening",
      value: "altening",
    },
    {
      label: "MCLeaks",
      value: "mcleaks",
    },
    {
      label: "Cracked",
      value: "cracked",
    },
  ]);

  const filteredAccounts = useMemo(() => {
    if (!search || !accounts) return accounts;

    const fuse = new Fuse(accounts, {
      keys: ["username", "email"],
    });

    return fuse.search(search).map((result) => result.item);
  }, [search, accounts]);

  return (
    <Menu>
      <Menu.Content>
        <Header>
          <SearchBar onChange={setSearch} />
          <Switch value={favoritesOnly} onChange={setFavoritesOnly}>
            Favorites Only
          </Switch>
          <Combobox
            options={types}
            onToggle={(option) =>
              setTypes((prev) =>
                prev.map((item) =>
                  item.value === option.value
                    ? { ...item, checked: !item.checked }
                    : item
                )
              )
            }
          >
            Type
          </Combobox>
        </Header>
        <List loading={status === "loading"} error={error?.message}>
          {filteredAccounts?.map((account) => (
            <AccountEntry key={account.username} account={account} />
          ))}
        </List>
      </Menu.Content>
      <Footer>
        <Footer.Actions>
          <LoginButton />
          <Button icon={Clipboard}>Clipboard</Button>
          <Button icon={Import}>Import</Button>
          <Button icon={Shuffle}>Random</Button>
          <Button icon={Check}>Check</Button>
        </Footer.Actions>
        <Footer.Back />
      </Footer>
    </Menu>
  );
}
