# Cobbleworkers

[![License: MPL-2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg?style=flat-square)](https://opensource.org/licenses/MPL-2.0)
[![modrinth-badge](https://img.shields.io/modrinth/dt/cobbleworkers?label=Modrinth&logo=Modrinth&style=flat-square)](https://modrinth.com/mod/cobbleworkers/versions)
[![curseforge-badge](https://img.shields.io/curseforge/dt/1296055?style=flat-square&logo=curseforge&label=CurseForge)](https://minecraft.curseforge.com/projects/1296055/files)

![cobbleworkers-icon](/src/main/resources/assets/cobbleworkers/icon.png)

Cobbleworkers is a companion server-side mod for Cobblemon `1.6.1` that turns the **Pasture Block** into a powerful utility block.

## Features

![cobbleworkers-infographic](src/main/resources/assets/cobbleworkers/infographic/infographic.png)

By default
- **Crop Irrigation** – Water-types keep crops hydrated.
- **Crop Harvesting** – Grass-types assist with crop harvesting.
- **Berry Collection** – Grass-types assist with berry harvesting.
- **Apricorn Harvesting** – Bug-types pick apricorns for you.
- **Amethyst Mining** – Rock-types collect amethyst clusters nearby.
- **Tumblestone Harvesting** – Steel-types collect tumblestone.
- **Honey Collection** – The combee line gathers honeycombs from beehives.
- **Mint Harvesting** - Fairy-types harvest mints.

Pokémon will automatically place all of the items in inventories nearby!

## Configuration
Each job can be customized via a config file. Enable/disable job types and specify which Pokémon can perform them.

Cobbleworkers uses [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config) and [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) for easy in-game tweaking
when using the minecraft integrated server, for servers use the config file.

Sample default config

```json
{
  "apricorn": {
    "apricornHarvestersEnabled": true,
    "apricornHarvesters": [
      "pikachu"
    ],
    "typeHarvestsApricorns": "BUG"
  },
  "irrigation": {
    "cropIrrigatorsEnabled": true,
    "cropIrrigators": [
      "pikachu"
    ],
    "typeIrrigatesCrops": "WATER"
  },
  "amethyst": {
    "amethystHarvestersEnabled": true,
    "amethystHarvesters": [
      "pikachu"
    ],
    "typeHarvestsAmethyst": "ROCK"
  },
  "tumblestone": {
    "tumblestoneHarvestersEnabled": true,
    "tumblestoneHarvesters": [
      "pikachu"
    ],
    "typeHarvestsTumblestone": "STEEL"
  },
  "cropHarvest": {
    "cropHarvestersEnabled": true,
    "cropHarvesters": [
      "pikachu"
    ],
    "typeHarvestsCrops": "GRASS"
  },
  "berries": {
    "berryHarvestersEnabled": true,
    "berryHarvesters": [
      "pikachu"
    ],
    "typeHarvestsBerries": "GRASS"
  },
  "honey": {
    "honeyCollectorsEnabled": true,
    "combeeLineCollectsHoney": true,
    "honeyCollectors": [
      "pikachu"
    ],
    "typeHarvestsHoney": "NONE"
  },
  "mints": {
    "mintHarvestersEnabled": true,
    "mintHarvesters": [
      "pikachu"
    ],
    "typeHarvestsMints": "FAIRY"
  }
}
```

## License
Licensed under [MPL-2.0](https://mozilla.org/MPL/2.0/)
