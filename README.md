# Thin Air — NeoForge 1.21.1 Port

A faithful NeoForge port of [fuzs's Thin Air](https://modrinth.com/mod/thin-air). The air is not always breathable deep underground and in other dimensions.

The original mod is [archived on Modrinth](https://modrinth.com/mod/thin-air/versions) (last release: Minecraft 1.20.4). This fork brings it forward to **Minecraft 1.21.1** on NeoForge while staying close to the upstream design (not a ground-up rewrite) with some features added.

> ⚠️ Please report bugs for this port **only** in [this repository's issue tracker](https://github.com/kgbcupcake/ThinAir-ReLived/issues). Do not contact fuzs about issues specific to this port.

---

## What's Different

- **NeoForge-only**: Direct port of the 1.20.4 codebase and data, updated for NeoForge 1.21.1 APIs and datapack path changes (`recipe/`, `advancement/`, `loot_table/`, and so on).
- **Server-authoritative config**: Settings live in `thinair-server.toml` under each world's `serverconfig/` folder, with an optional in-game config screen (Mods menu) via Cloth Config.
- **Optional Curios**: Respirator uses the Curios head slot and Safety Lantern uses the Curios belt slot when [Curios](https://modrinth.com/mod/curios) is installed; the mod works without it.
- **Optional Create integration**: Backtanks provide breathable air in yellow and red air when worn with a Diving Helmet (via [Create](https://modrinth.com/mod/create)'s own equipment system, with an optional Curios back slot for the backtank itself); like Curios, none of this is required or bundled — the mod loads and works fine with neither installed.
- **Work in progress**: Full parity testing is ongoing.

---

## Features

- **Air quality levels**: Green, yellow, red, and blue air affect breathing and what equipment helps, by dimension and height.
- **Safety Lantern**: Shows nearby air quality by color; can be dyed and scraped with an axe.
- **Signal Torch**: Right-click to emit particles (configurable).
- **Respirator**: Protects against choking air (Curios head slot when available).
- **Air Bladder** / **Reinforced Air Bladder**: Portable air refill; reinforced variant crafted with netherite and copies durability from the ingredient bladder.
- **Bottle of Soulfire**: Emergency air restore in the Nether.
- Advancements, recipes, loot injections, drowned air-drain behavior, chunk air-quality sync, and English / Russian / Chinese lang files.

### Blocks

- **Safety Lantern**: shows nearby air quality by color; can be dyed and scraped with an axe
- **Signal Torch**: right-click to emit particles (configurable)

### Items

- **Respirator**: protects against choking air (Curios head slot when available)
- **Air Bladder** / **Reinforced Air Bladder**: portable air refill (reinforced recipe copies bladder durability)
- **Bottle of Soulfire**: emergency air restore in the Nether

### Advancements

- **Air Bladder**: use an Air Bladder to refill your air supply on the go
- **Blue Air**: breathe the life force given off by Soul Fire to maintain your air (but not increase it!)
- **Disco Lantern**: use a piece of Dye to manually change the color of a Safety Lamp (you can scrape the color off with an axe)
- **Respirator**: protect yourself from choking air with something like a Respirator
- **Safety Lantern** — use a Safety Lantern to check the air quality around you
- **Signal Torch**: right-click a torch to make it spew particles, perhaps to signal the exit of a cave
- **Soulfire Bottle**: restore your lungs with the souls trapped in a Bottle of Soulfire
- **Water Breathing**: breathe freely where there is no air at all

## Known limitations

Work still in progress compared to the original 1.20.4 release:

- Full parity testing (multiplayer, loot, advancements, worldgen-placed air providers) is ongoing

---
## Technical Info

| Dependency   | Version          | Notes                                                                 |
| ------------ | ---------------- | --------------------------------------------------------------------- |
| NeoForge     | 1.21.1           | Required                                                              |
| Java         | 21               | Required                                                              |
| [Curios](https://modrinth.com/mod/curios) | 9.5.1+1.21.1 | Optional: respirator head slot, belt lantern, Curios rendering |
| [Create](https://modrinth.com/mod/create) | 6.0.10-280 | Optional: backtank breathable air via Diving Helmet |
| [Cloth Config](https://modrinth.com/mod/cloth-config) | 15.0.140 | Recommended: in-game config screen from the Mods menu; otherwise edit `serverconfig/thinair-server.toml` |

## Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1.
2. Download or build `thinair-*.jar` from [Releases](https://github.com/kgbcupcake/ThinAir-ReLived/releases).
3. Drop the mod `.jar` into your `mods/` folder.
4. Optionally install [Curios](https://modrinth.com/mod/curios) for respirator/lantern slot support, and/or [Create](https://modrinth.com/mod/create) for backtank breathable-air support.
5. Launch the game.

After the first launch, change settings from the Mods menu config screen or by editing `<world>/serverconfig/thinair-server.toml` (air quality, signal torches, drowned choking, and air-provider bubble ranges).

---

## Credits

| Role                 | Author                                              |
| -------------------- | --------------------------------------------------- |
| Original mod         | [fuzs](https://modrinth.com/mod/thin-air)           |
| 1.21.1 NeoForge port | [Marie (kgbcupcake)](https://github.com/kgbcupcake) |

---

## References

- [Changelog](CHANGELOG.md)
- [Original mod on Modrinth](https://modrinth.com/mod/thin-air)
- [Original source](https://github.com/Fuzss/thinair)
- [Curios](https://modrinth.com/mod/curios)
- [Create](https://modrinth.com/mod/create)

---

## License

MIT — same as the [original Thin Air project](https://modrinth.com/mod/thin-air). Asset rights follow the upstream [LICENSE-ASSETS](https://github.com/Fuzss/modresources) terms where applicable.
