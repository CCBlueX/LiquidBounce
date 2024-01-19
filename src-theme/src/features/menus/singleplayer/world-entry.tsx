import { World } from "~/utils/api";
import { ListItem } from "..";

type WorldEntryProps = {
  world: World;
};

export default function WorldEntry({ world }: WorldEntryProps) {
  return (
    <ListItem>
      {/* Server Icon Wrapper */}
      <div className="relative h-[68px] w-[68px]">
        {/* Server Icon */}
        <img src={`${world.icon}`} alt="World Icon" className="rounded-full" />
      </div>

      {/* Metadata */}
      <div className="flex flex-col space-y-1">
        {/* Server Name Wrapper */}
        <div className="flex space-x-2">
          <div className="text-white text-xl font-semibold uppercase">
            {world.name}
          </div>
        </div>

        {/* Server MOTD */}
        <div className="text-white/50 text-xl font-semibold">
          {new Date(world.lastPlayed).toLocaleString()}
        </div>
      </div>
    </ListItem>
  );
}
