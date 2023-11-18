import { ResizableBox } from "react-resizable";

import { useAlignmentGrid } from "./alignment-grid";

import "./resizable.css";

type ResizableProps = {
  children: React.ReactNode;
  minHeight?: number;
  minWidth?: number;
  maxWidth?: number;
  maxHeight?: number;
  width: number;
  height: number;
} & React.ComponentProps<typeof ResizableBox>;

export default function Resizable({
  width,
  height,
  minHeight = 0,
  minWidth = 0,
  maxWidth = Infinity,
  maxHeight = Infinity,
  children,
  ...props
}: ResizableProps) {
  const { enabled, horizontal, vertical } = useAlignmentGrid();

  return (
    <ResizableBox
      {...props}
      draggableOpts={enabled ? { grid: [horizontal, vertical] } : undefined}
      minConstraints={[minWidth, minHeight]}
      maxConstraints={[maxWidth, maxHeight]}
      height={height}
      width={width}
    >
      {children}
    </ResizableBox>
  );
}
