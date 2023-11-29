import { ListItem } from "..";

import { Proxy } from "~/utils/types";

type ProxyEntryProps = {
  proxy: Proxy;
};

export default function ProxyEntry({ proxy }: ProxyEntryProps) {
  return (
    <ListItem>
      {/* Proxy Location Wrapper */}
      <div className="relative h-[68px] w-[68px]">
        {/* Proxy Location */}
        <img
          src={`./flags/${proxy.location}.svg`}
          alt="Proxy Location"
          className="rounded-full"
        />
      </div>

      {/* Metadata */}
      <div className="flex flex-col space-y-1">
        {/* Server Name Wrapper */}
        <div className="flex space-x-2">
          <div className="text-white text-xl font-semibold uppercase">
            {proxy.username}
          </div>
        </div>

        {/* Server MOTD */}
        <div className="text-white/50 text-xl font-semibold">
          {proxy.host}:{proxy.port}
        </div>
      </div>
    </ListItem>
  );
}
