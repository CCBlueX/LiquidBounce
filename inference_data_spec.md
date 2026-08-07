# Enshuu-Neo Inference Data Processing Spec

This spec defines exactly how to transform `TrainDataCollectorSpec.md` parquet rows into model-ready tensors for inference (including Java/DJL use).

It mirrors `data_pipeline.py` + `train.py` behavior used during training, so inference uses identical feature semantics.

## 1) Raw Input Contract

Input rows come from `TrainDataCollectorSpec.md` and include, per player and tick:
- kinematics and rotation: `posX,posY,posZ,yaw,pitch`
- booleans: `isSneaking,isOnGround,isUsingItem,isSwinging,wasHit`
- nearest projectile rel vector: `closestArrowX,closestArrowY,closestArrowZ`
- hand categories: `mainHandCategory,offHandCategory`
- maps: `floorMap,ceilMap,poiMap` (flattened 15x15 arrays)
- identity/time: `playerId,timestamp`

For inference, rows MUST be grouped by `playerId` and sorted by `timestamp`.

## 2) Temporal Resolution

Model training expects 5Hz sequences (downsampled from 20Hz):
- `DOWNSAMPLE_FACTOR = 4`
- Keep every 4th frame (`tick_idx % 4 == 3`)
- For `isSwinging` and `wasHit`, OR-aggregate over the 4-tick window

If your runtime source already provides stable 5Hz frames with equivalent semantics, skip downsampling.

## 3) Sequence Segmentation Rules

Per-player streams are split into contiguous sequences.
Break sequence when either:
- timestamp discontinuity (`dt != 4` in original 20Hz tick units), or
- teleport/abrupt movement (`euclidean_step_delta > 7.0` blocks per 5Hz step)

Only segments with at least `MIN_CONTEXT_FRAMES = 10` are valid.

## 4) Per-Frame Feature Engineering (exact order)

All computations below are in `preprocess_sequence()`.

### 4.0 Coordinate-frame legend (what is view-relative vs not)

`View-relative` means values are expressed in the local player frame at that tick:
- +X: left/right relative to current yaw (strafe axis)
- +Z: forward/back relative to current yaw (look axis)
- Y: vertical axis (same world up/down axis, not yaw-rotated)

`World-relative` means values are in absolute world coordinates or world-frame deltas.

**Raw collector fields**
- World-relative: `posX,posY,posZ`, `yaw`, `pitch`, `closestArrowX,closestArrowY,closestArrowZ`
- Categorical/non-geometric: `mainHandCategory,offHandCategory`, booleans, maps

**Model tensors after preprocessing**
- View-relative: `view_dx,view_dz,last_view_dx,last_view_dz,arr_dir_x,arr_dir_z,acc_x,acc_z`, labels (`deltaX,deltaY,deltaZ` in output space)
- Vertical/world-axis: `view_dy,last_dy,arr_dir_y,acc_y` (not yaw-rotated, but still in model feature vector)
- World-frame derived but scalar: `velocity`, `arrow_dist`
- Rotation values (not position-frame vectors): `d_yaw,d_pitch,sin_yaw,cos_yaw,sin_pitch,cos_pitch`

### 4.1 View-space rotation helper
Given world deltas `(dx,dz)` and yaw `yaw`:
- `rx = dx*cos(-yaw) - dz*sin(-yaw)`
- `rz = dx*sin(-yaw) + dz*cos(-yaw)`

### 4.2 Base scalar feature vector (25 dims)
For each frame `t`, build `scalar_features[t]` in this exact order:

1. `view_dx`
2. `view_dy`
3. `view_dz`
4. `last_view_dx`     (delta from last frame in sequence, in view X)
5. `last_dy`          (world Y delta from last frame)
6. `last_view_dz`     (delta from last frame in sequence, in view Z)
7. `velocity`         (`sqrt(raw_dx^2 + raw_dy^2 + raw_dz^2)`)
8. `d_yaw`            (wrapped to `[-pi, pi]`)
9. `d_pitch`
10. `sin_yaw`
11. `cos_yaw`
12. `sin_pitch`
13. `cos_pitch`
14. `is_sneaking`
15. `is_using_item`
16. `is_on_ground`
17. `was_hit`
18. `is_swinging`
19. `arr_dir_x`
20. `arr_dir_y`
21. `arr_dir_z`
22. `arrow_dist`
23. `acc_x`           (`diff(view_dx)`)
24. `acc_y`           (`diff(view_dy)`)
25. `acc_z`           (`diff(view_dz)`)

Notes:
- Arrow direction is normalized; if distance `< 0.01`, set dir=(0,0,0), dist=0.
- First-frame deltas/accels are zero-initialized.

### 4.3 Maps
For each frame:
- `floorMap` decode: `float32(floorMap) / 256.0`, reshape `[15,15]`
- `ceilMap` decode: `float32(ceilMap) / 256.0`, reshape `[15,15]`
- `poiMap` reshape `[15,15]`, clamp categories to `[0,19]`

### 4.4 Hand categories
- `main_hand = clamp(mainHandCategory, 0, 15)`
- `off_hand = clamp(offHandCategory, 0, 15)`

## 5) Context Window Construction for Inference

For one inference query, provide one context sequence of length `T`:
- Recommended `10 <= T <= 50`
- Model max sequence length is config-dependent (`cfg.tf.max_seq_len`, default 64)
- If longer than max, keep the most recent `max_seq_len` frames

Package tensors:
- `floor_maps`: `[B,T,15,15]` float32
- `ceil_maps`: `[B,T,15,15]` float32
- `poi_maps`: `[B,T,15,15]` int64
- `scalar_features`: `[B,T,25]` float32
- `main_hand`: `[B,T]` int64
- `off_hand`: `[B,T]` int64
- `y_horizons`: `[B,L]` int64 (values 1..15)
- `gamemode`: `[B]` int64
- `ctx_lens`: `[B]` int64 (true unpadded lengths)

`B` is batch size, `L` is number of requested horizons.

## 6) Model Output Contract

MDN output tensors:
- `pi_logits`: `[B,L,K]`
- `mu`: `[B,L,K,3]`
- `sigma`: `[B,L,K,3]` (strictly positive)

Where:
- `K = mixture_components` (default 5)
- XYZ are view-relative deltas from context endpoint.

## 7) Runtime Prediction Strategies

Given model output for one horizon:
- Mixture weights: `pi = softmax(pi_logits)`
- Best-mode point estimate (used in validation decision): choose `k* = argmax(pi)` and predict `mu[k*]`
- Expected-value estimate: `sum_k pi[k] * mu[k]`

For shot probability within tolerance box around chosen aim point:
- Line from the players eye to the target (on the XZ plane)
- Tolerance (project-specific): right left from the line: 0.3, Y=0.8, in direction/the oppsite direction of the line 3.0
- Assuming independent axes for selected Gaussian:
  - `P_axis = erf(tol_axis / (sqrt(2)*sigma_axis))`
  - `P_hit = P_x * P_y * P_z`
- Take shot if `P_hit > threshold` (e.g. `0.25`).

## 8) Consistency Requirements (important)

To avoid train/infer skew:
- Use identical downsampling logic and event OR aggregation
- Use identical scalar-feature order and map decoding scale
- Clamp POI and hand categories exactly as above
- Use same horizon semantics (`1..15` at 5Hz)
- Use same view-space coordinate system

## 9) Java/DJL Mapping Notes

In DJL, feed NDArrays with exact dtypes/shapes from section 5.
If using exported TorchScript wrapper (`export_djl_model.py`), input order is:
1) `floor_maps`
2) `ceil_maps`
3) `poi_maps`
4) `scalar_features`
5) `main_hand`
6) `off_hand`
7) `y_horizons`
8) `gamemode`
9) `ctx_lens`
