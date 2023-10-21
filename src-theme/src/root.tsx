import { Outlet } from "react-router-dom";

import { TooltipProvider } from "./components/Tooltip";

export default function Root() {
  return (
    <TooltipProvider>
      <Outlet />
    </TooltipProvider>
  );
}
