import { cva } from "class-variance-authority";

const latencyPill = cva(
  "rounded-full px-2 py-1 text-white text-[10px] font-bold",
  {
    variants: {
      latency: {
        low: "bg-green-500",
        medium: "bg-yellow-500",
        high: "bg-red-500",
      },
    },
  }
);

type LatencyPillProps = {
  latency: number;
  className?: string;
};

export default function LatencyPill({ latency, className }: LatencyPillProps) {
  const latencyClass =
    latency < 100 ? "low" : latency < 200 ? "medium" : "high";

  return (
    <div
      className={latencyPill({
        latency: latencyClass,
        className,
      })}
      data-latency={latency}
    >
      {latency}ms
    </div>
  );
}
