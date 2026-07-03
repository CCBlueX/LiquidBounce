export const HUD_EDITOR_GRID_SIZE = 10;

export const HORIZONTAL_ANCHOR_ZONES = ["left", "center", "right"] as const;
export const VERTICAL_ANCHOR_ZONES = ["upper", "center", "lower"] as const;

export type HorizontalAnchorZone = typeof HORIZONTAL_ANCHOR_ZONES[number];
export type VerticalAnchorZone = typeof VERTICAL_ANCHOR_ZONES[number];

export interface HudEditorDragState {
    dragging: boolean;
    horizontalZone: HorizontalAnchorZone;
    verticalZone: VerticalAnchorZone;
}
