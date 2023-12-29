import { useEffect } from "react";
import { QueryClient, QueryClientProvider } from "react-query";
import { Outlet, useNavigate } from "react-router-dom";

import { TooltipProvider } from "./components/tooltip";
import { useWebsocket } from "./contexts/websocket-context";

type VirtualScreenData = {
  screenName: string;
  action: "close" | "open";
};

export function Component() {
  const { listen } = useWebsocket();
  const navigate = useNavigate();

  function updateScaleFactor(windowWidth: number, windowHeight: number) {
    const baseResolutionWidth = 854;
    const baseResolutionHeight = 480;
    const baseScaleFactor = 0.35;
    const maxResolutionWidth = 1920 * 1.1;
    const maxResolutionHeight = 1080 * 1.1;

    localStorage.clear();

    // Calculate the aspect ratio of the base resolution
    const baseAspectRatio =
      Math.min(baseResolutionWidth, baseResolutionHeight) /
      Math.max(baseResolutionWidth, baseResolutionHeight);

    // Calculate the aspect ratio of the current window
    const currentAspectRatio =
      Math.min(windowWidth, windowHeight) / Math.max(windowWidth, windowHeight);

    // Calculate the scale factor based on the aspect ratios
    let scaleFactor = baseScaleFactor * (baseAspectRatio / currentAspectRatio);

    // Adjust the scale factor to smoothly approach 1 as the resolution increases
    if (
      windowWidth > baseResolutionWidth &&
      windowHeight > baseResolutionHeight
    ) {
      const widthFactor = Math.min(
        1,
        (windowWidth - baseResolutionWidth) /
          (maxResolutionWidth - baseResolutionWidth)
      );
      const heightFactor = Math.min(
        1,
        (windowHeight - baseResolutionHeight) /
          (maxResolutionHeight - baseResolutionHeight)
      );
      const transitionFactor = Math.max(widthFactor, heightFactor);
      scaleFactor += (1 - baseScaleFactor) * transitionFactor;
    }

    // Ensure the scale factor is at most 1
    scaleFactor = Math.min(scaleFactor, 1);
    document.documentElement.style.setProperty(
      "--scale-factor",
      scaleFactor.toString()
    );
  }

  useEffect(() => {
    const virtualScreenCleanup = listen(
      "virtualScreen",
      ({ action, screenName }: VirtualScreenData) => {
        console.log("virtualScreen", action, screenName);

        if (action === "close") {
          navigate("/closed");
        } else if (action === "open") {
          navigate(`/${screenName}`);
        }
      }
    );

    function handleResize() {
      updateScaleFactor(window.innerWidth, window.innerHeight);
    }

    window.addEventListener("resize", handleResize);

    handleResize();

    return () => {
      virtualScreenCleanup();
      window.removeEventListener("resize", handleResize);
    };
  }, [listen, navigate]);

  const queryClient = new QueryClient();

  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Outlet />
      </TooltipProvider>
    </QueryClientProvider>
  );
}

Component.displayName = "Root";
