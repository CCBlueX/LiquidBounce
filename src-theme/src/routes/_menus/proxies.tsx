import Fuse from "fuse.js";
import { useMemo, useState } from "react";

import Button from "~/components/button";
import Combobox, { type Option } from "~/components/combobox";
import Switch from "~/components/switch";

import Header from "~/features/menus/header";
import Footer from "~/features/menus/footer";
import List from "~/features/menus/list";
import Menu from "~/features/menus/menu";
import SearchBar from "~/features/menus/searchbar";

import ProxyEntry from "~/features/menus/proxies/proxy-entry";
import { useProxies } from "~/features/menus/proxies/use-proxies";

// Left Footer Buttons
import { ReactComponent as Add } from "~/assets/icons/add.svg";
import { ReactComponent as Clipboard } from "~/assets/icons/clipboard.svg";
import { ReactComponent as Import } from "~/assets/icons/import.svg";
import { ReactComponent as Shuffle } from "~/assets/icons/shuffle.svg";
import { ReactComponent as Check } from "~/assets/icons/check.svg";

export function Component() {
  const { proxies } = useProxies();
  const [search, setSearch] = useState("");
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [types, setTypes] = useState<Option[]>([
    {
      label: "SOCKS4",
      value: "socks4",
    },
    {
      label: "SOCKS5",
      value: "socks5",
    },
    {
      label: "HTTP",
      value: "http",
    },
  ]);

  const filteredProxies = useMemo(() => {
    if (!search) return proxies;

    const fuse = new Fuse(proxies, {
      keys: ["host", "port", "username"],
    });

    return fuse.search(search).map((result) => result.item);
  }, [search, proxies]);

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
        <List>
          {filteredProxies.map((proxy) => (
            <ProxyEntry
              key={[proxy.host, proxy.port, proxy.username].join(":")}
              proxy={proxy}
            />
          ))}
        </List>
      </Menu.Content>
      <Footer>
        <Footer.Actions>
          <Button icon={Add}>Add</Button>
          <Button icon={Clipboard}>Copy</Button>
          <Button icon={Import}>Import</Button>
          <Button icon={Shuffle}>Random</Button>
          <Button icon={Check}>Check</Button>
        </Footer.Actions>
        <Footer.Back />
      </Footer>
    </Menu>
  );
}

Component.displayName = "ProxyManager";
