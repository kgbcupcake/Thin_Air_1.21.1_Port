# Changelog

All notable changes to this **NeoForge 1.21.1 port** of [Thin Air](https://modrinth.com/mod/thin-air) are documented here.

This fork is maintained separately from [fuzs's original mod](https://github.com/Fuzss/thinair) (last release: Minecraft 1.20.4). For history prior to the port, see the [original Modrinth versions page](https://modrinth.com/mod/thin-air/versions).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Optional **[Create](https://modrinth.com/mod/create)** integration: backtanks provide breathable air in yellow and red air when worn via Create's own armor slot (`BacktankUtil`)

- `back` Curios slot granted to the player entity so Create backtanks have somewhere to go

### Fixed

- Curios tag datapack paths were incorrectly changed to `curios/tags/items/` (plural), which silently broke the `curios:head`/`curios:belt`/`curios:back` tags and stopped the Respirator, Safety Lantern, and Create backtanks from being recognized as valid curios; item tags in 1.21.1 use the singular `curios/tags/item/` (matching this mod's own `data/thinair/tags/item/`), so the folder has been reverted
- Player entity was never granted the `back` slot, so Create backtanks had no valid Curios slot to equip into even once the tag was fixed
- `green_air_providers`/`red_air_providers` block tags were missing nether/end portals and lava/fire, so they no longer acted as breathable/hazardous air sources like in the original mod; restored to match upstream 1.20.4 (`minecraft:end_portal`, `minecraft:nether_portal`, `minecraft:end_gateway` for green; `minecraft:lava`, `minecraft:fire` for red) ([#2](https://github.com/kgbcupcake/ThinAir-ReLived/issues/2))
- Create backtanks were granting red/yellow air protection just by being carried, with no Diving Helmet required; now requires `DivingHelmetItem.isWornBy(entity)` before consuming backtank air, matching Create's own behavior ([#3](https://github.com/kgbcupcake/ThinAir-ReLived/issues/3))
- Create backtank air could drain twice as fast as intended: this mod's and Create's own `LivingBreatheEvent` listeners both independently drained the same backtank on the same tick when Create's listener happened to run first; ThinAir's listener now runs at `HIGH` priority so it always resolves breathability before Create's listener checks it, guaranteeing a single drain ([#5](https://github.com/kgbcupcake/ThinAir-ReLived/issues/5))

### Changed

- Create listed as an optional dependency in `neoforge.mods.toml`

### Known limitations

- **Create backtanks do not work in a Curios back slot** — Create only exposes backtanks through its own armor/chest equipment path; a Curios bridge was attempted (`CreateCuriosCompat`) but cannot fully replicate Create's behavior outside that slot

## [1.0.3] - 2026-06-14

### Changed

- Mod display name rebranded to **ThinAir-ReLived**
- Modrinth listing updated to [thinair-relived](https://modrinth.com/mod/thinair-relived)
- Project home moved to [kgbcupcake/ThinAir-ReLived](https://github.com/kgbcupcake/ThinAir-ReLived)
- `.gitignore` replaced with a NeoForge-focused ruleset; Gradle cache, IDE folders, and `build/` output are no longer tracked in git

## [1.0.2] - 2026-06-14

### Added

- `fuzs.thinair` package `SafetyLanternBlock` class for upstream mixin / compatibility paths

### Changed

- Author metadata and Modrinth `displayURL` in mod metadata

## [1.0.1] - 2026-06-08

### Added

- In-game config screen (Cloth Config) for server settings from the Mods menu
- Config load/reload binding so `thinair-server.toml` settings apply correctly on world startup and reload
- **Safety Lantern belt slot** via Curios (optional alternative to carrying in inventory)
- Curios belt lantern renderer on the player model (front-mounted, with armor offset)
- Server-authoritative **player air quality sync** (`ClientboundPlayerAirQualityPacket`) for held/belt lantern display in multiplayer
- Client-side `ClientPlayerAirQualityCache` and `LanternDisplayResolver` so item model properties read cached state instead of sampling air quality every render frame
- Reactive placed-lantern updates in `AirBubbleTracker` (dirty-chunk drain when air bubbles change; no scheduled block ticks)
- `minecraft:tags/block/wall_post_override` entry for signal torch (vanilla-compatible wall placement)
- `minecraft:tags/block/mineable/pickaxe` entry for safety lantern (correct break speed and drops)
- Cutout render layers for signal torch, wall signal torch, and safety lantern blocks
- Curios player entity slot assignment (`head` + `belt`) and explicit head/belt slot type registration via InterModComms
- Modrinth `displayURL` in `neoforge.mods.toml`

### Changed

- Advancement trigger updates for Minecraft 1.21.1 datapack format
- **Respirator advancement** now requires breathing yellow air and equipping the respirator in the Curios head slot (not just receiving the item)
- **Safety Lantern advancement** requires breathing yellow/red air while holding a lantern
- `air_quality_sensitive` entity tag restored to upstream parity (players, villagers, wandering traders, and illagers)
- `yellow_air_providers` block tag emptied to match upstream (torches/campfires/lanterns no longer create yellow air bubbles)
- `heavy_breathing_equipment` item tag emptied to match upstream (respirator protects in yellow air only, not red)
- Air bladder refill logic runs server-side only (fixes hold-to-refill in thin air)
- Reinforced Air Bladder crafting copies durability from the ingredient air bladder
- Chunk air-bubble scanning conditions corrected (removed debug `false ||` / `true ||` overrides)

### Fixed

- Safety Lantern could not be mined with a pickaxe or would not drop as an item when broken
- Curios GUI did not appear when Thin Air was the only mod registering Curios slots
- Held and belt Safety Lantern colors did not update with ambient air quality
- Placed Safety Lantern block colors could stay stale after nearby air-provider blocks changed
- Signal torch wall placement could behave differently from vanilla torches
- Possible transparency/rendering glitches on torches and lanterns without cutout render layers
- Race condition in `AirBubbleTracker` dirty-chunk processing under concurrent access

### Removed

- Accidental `repomix-output.xml` from the source tree

### Known limitations

- Full parity testing in generated structures (loot injections, worldgen-placed air providers) is ongoing
- Multiplayer edge cases (remote players, chunk unload/reload) have not been exhaustively verified
- Report bugs for **this port only** in [this repository's issue tracker](https://github.com/kgbcupcake/ThinAir-ReLived/issues)

## [1.0.0] - 2026-06-08

First release of the NeoForge 1.21.1 port by [Marie (kgbcupcake)](https://github.com/kgbcupcake).

### Added

- **NeoForge 1.21.1 port** of Thin Air, updated from the upstream 1.20.4 codebase and data
- **Air quality system**: green, yellow, red, and blue air by dimension and height, with breathing equipment interactions
- **Safety Lantern**: shows nearby air quality by color; dyeable and color can be scraped off with an axe
- **Signal Torch** and **Wall Signal Torch**: right-click to emit particles (toggleable via config)
- **Respirator**: protects against choking air; uses the Curios head slot when [Curios](https://modrinth.com/mod/curios) is installed
- **Air Bladder** and **Reinforced Air Bladder**: portable air refill items
- **Bottle of Soulfire**: emergency air restore in the Nether
- **Reinforced Air Bladder crafting**: shapeless netherite + air bladder recipe with durability copied from the ingredient bladder
- **Drowned behavior**: drowned can drain player air when attacking (configurable)
- **Chunk air-quality sync**: client receives air quality updates per chunk for air-provider bubbles
- **Air provider bubbles**: configurable radius for green, yellow, red, and blue air sources
- **Advancements**: air bladder, blue air, disco lantern, respirator, safety lantern, signal torch, soulfire bottle, water breathing
- **Recipes**: all core crafting recipes ported to 1.21.1 `recipe/` datapack layout
- **Loot injections**: safety lanterns in dungeon, mineshaft, and stronghold chests; soulfire bottles in buried treasure, shipwreck, and ruin chests
- **Server config**: `thinair-server.toml` under each world's `serverconfig/` folder
- **Localization**: English, Russian, and Chinese lang files
- **Optional Curios integration**: respirator head slot and client rendering when Curios is present

### Changed

- Datapack paths updated for Minecraft 1.21.1 (`recipe/`, `advancement/`, `loot_table/`, and related renames)
- NeoForge-only loader (no dual Forge/NeoForge build)
- Package namespace `dev.maire.thinair` for the port

### Removed

- Bundled compatibility layers for Create and other mods (compatibility remains optional and separate, matching port goals)

[Unreleased]: https://github.com/kgbcupcake/ThinAir-ReLived/compare/v1.0.3...HEAD
[1.0.3]: https://github.com/kgbcupcake/ThinAir-ReLived/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/kgbcupcake/ThinAir-ReLived/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/kgbcupcake/ThinAir-ReLived/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/kgbcupcake/ThinAir-ReLived/releases/tag/v1.0.0
