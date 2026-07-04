export const HUD_EDITOR_GRID_SIZE = 10;
export const HUD_EDITOR_MAGNET_THRESHOLD = 6;
export const HUD_EDITOR_ELEMENTS_CONTEXT = Symbol("hud-editor-elements");

export const HORIZONTAL_ANCHOR_ZONES = ["left", "center", "right"] as const;
export const VERTICAL_ANCHOR_ZONES = ["upper", "center", "lower"] as const;

export type HorizontalAnchorZone = typeof HORIZONTAL_ANCHOR_ZONES[number];
export type VerticalAnchorZone = typeof VERTICAL_ANCHOR_ZONES[number];

export interface HudEditorDragState {
    dragging: boolean;
    horizontalZone: HorizontalAnchorZone;
    verticalZone: VerticalAnchorZone;
    verticalGuide?: number;
    horizontalGuide?: number;
    magneticTargetIds: string[];
}
