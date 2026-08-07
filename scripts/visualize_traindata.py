#!/usr/bin/env python3
import argparse
import sys
import time
import os
import math

try:
    import pandas as pd
    import numpy as np
except ImportError:
    print("Error: pandas and numpy are required.")
    print("Please install them using: pip install pandas numpy pyarrow fastparquet")
    sys.exit(1)

def clear_screen():
    os.system('cls' if os.name == 'nt' else 'clear')

def format_grid(arr, title, is_heightmap=False):
    if not isinstance(arr, (list, np.ndarray)) or len(arr) != 225:
        return f"Invalid grid data for {title}"

    grid = np.array(arr).reshape(15, 15)
    
    # Simple ASCII rendering
    lines = [f"--- {title} ---"]
    
    for z in range(15):
        row_chars = []
        for x in range(15):
            val = grid[z, x]
            if is_heightmap:
                # Convert fixed point back to float
                real_val = val / 256.0
                if real_val < -9.9: # Floor miss
                    row_chars.append(" X ")
                elif real_val > 3.9: # Ceil miss
                    row_chars.append(" X ")
                else:
                    # Simple color/character scaling
                    if real_val < -2: row_chars.append(".. ")
                    elif real_val < 0: row_chars.append("-- ")
                    elif real_val < 2: row_chars.append("++ ")
                    else: row_chars.append("## ")
            else:
                # POI Map
                if val == 0:
                    row_chars.append(" . ")
                else:
                    row_chars.append(f"{val:2d} ")
        
        # Player marker in the center
        if z == 7:
            row_chars[7] = " P "
            
        lines.append("".join(row_chars))
    
    return "\n".join(lines)

def main():
    parser = argparse.ArgumentParser(description="Visualize TrainDataCollector Parquet files.")
    parser.add_argument("file", help="Path to the .parquet file")
    parser.add_argument("--fps", type=float, default=5.0, help="Playback speed in frames per second (default 5.0)")
    parser.add_argument("--uuid", type=str, help="Specific player UUID to track. If omitted, picks the first one found.")
    
    args = parser.parse_args()

    try:
        df = pd.read_parquet(args.file)
    except Exception as e:
        print(f"Failed to read parquet file: {e}")
        sys.exit(1)
        
    if df.empty:
        print("File contains no data.")
        sys.exit(0)

    # Filter by UUID if provided, else take the first UUID
    player_id = args.uuid
    if not player_id:
        player_id = df['playerId'].iloc[0]
        print(f"No UUID provided. Tracking first player found: {player_id}")
        time.sleep(2)
        
    df_player = df[df['playerId'] == player_id].sort_values('timestamp')
    
    if df_player.empty:
        print(f"No data found for player {player_id}")
        print("Available players:")
        print(df['playerId'].unique())
        sys.exit(1)

    print(f"Loaded {len(df_player)} frames for player {player_id}")
    
    sleep_time = 1.0 / args.fps

    for idx, row in df_player.iterrows():
        clear_screen()
        print(f"Frame: {idx} | Timestamp: {row['timestamp']} | Player: {player_id}")
        print("=" * 60)
        
        yaw_deg = math.degrees(row['yaw'])
        pitch_deg = math.degrees(row['pitch'])
        
        print(f"Position : X: {row['posX']:.2f}, Y: {row['posY']:.2f}, Z: {row['posZ']:.2f}")
        print(f"Rotation : Yaw: {yaw_deg:.1f}°, Pitch: {pitch_deg:.1f}°")
        print(f"State    : Sneak: {row['isSneaking']}, Ground: {row['isOnGround']}, Using: {row['isUsingItem']}, Swing: {row['isSwinging']}, Hit: {row['wasHit']}")
        print(f"Items    : Main: {row['mainHandCategory']}, Off: {row['offHandCategory']}")
        print(f"Closest Projectile: ({row['closestArrowX']:.2f}, {row['closestArrowY']:.2f}, {row['closestArrowZ']:.2f})")
        print("=" * 60)
        
        print(format_grid(row['floorMap'], "Floor Map (Relative Height)", is_heightmap=True))
        print("")
        print(format_grid(row['ceilMap'], "Ceil Map (Relative Height)", is_heightmap=True))
        print("")
        print(format_grid(row['poiMap'], "POI Map (Priorities)", is_heightmap=False))
        
        time.sleep(sleep_time)

if __name__ == "__main__":
    main()
