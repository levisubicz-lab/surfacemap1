# Surface Map (client-side Fabric mod)
- M opens a fullscreen, top-down SURFACE map (heightmap based; caves can never appear).
- Overworld + The End only. Anywhere else (Nether, modded dims) shows "Signal Lost - No Map Available".
- No HUD minimap, no entity radar, no teleporting (none of that code exists).
- Waypoints: right-click map = new, click marker = edit/delete. Stored in config/surfacemap/<server>.json.
- Client-only: works on Purpur (or any server) with nothing installed server-side.
- Do NOT run alongside Xaero's mods (they'd bring back the minimap/radar/cave mode).
- Terrain cache is in-memory (resets on disconnect). Waypoints persist.
