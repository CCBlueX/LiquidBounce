import * as SwitchPrimitive from "@radix-ui/react-switch";

type SwitchProps = {
  children: React.ReactNode;
  value: boolean;
  onChange: (value: boolean) => void;
};

export default function Switch({ children, value, onChange }: SwitchProps) {
  return (
    <div className="flex items-center space-x-5">
      <SwitchPrimitive.Root
        className="w-[28px] h-[14px] group rounded-full relative bg-white/30 data-[state=checked]:bg-brand/50 outline-none cursor-default transition-colors"
        checked={value}
        onCheckedChange={onChange}
      >
        <SwitchPrimitive.Thumb className="absolute top-1/2 -translate-y-1/2 -translate-x-[8px] block w-5 h-5 bg-brand rounded-full transition-transform duration-100 will-change-transform data-[state=checked]:translate-x-[16px] group-focus:ring-4 group-focus:ring-white" />
      </SwitchPrimitive.Root>
      <div className="text-white text-xl font-semibold">{children}</div>
    </div>
  );
}
