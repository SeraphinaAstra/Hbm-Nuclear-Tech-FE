# Changelog

## ⚠️ Breaking Changes

### Radiation System Overhaul

The radiation system has been substantially redesigned. Existing radiation-related gameplay, protection setups, and strategies may behave differently after updating.

* **Shielding System:** Replaced the previous shielding behavior with a material-based Half-Value Layer (HVL) system.
* **Radiation Sources:** Point sources now use directional occlusion raycasting with inverse-square falloff.
* **ARS:** Reworked the high-radiation death system with new dose bands, persistent death timers, and natural dose decay.
* **Radiation Treatment:** RadAway and Player Decontaminators now extend death timers instead of reducing radiation dose after lethal exposure has been committed.
* **Cancer:** Added persistent cancer burden and stochastic cancer risk based on accumulated radiation exposure.
* **Environmental Radiation:** Migrated several environmental point sources to the new occlusion-based radiation system.

> Existing worlds should remain loadable, but radiation exposure, shielding effectiveness, radiation safety strategies, and the behavior of radiation-related equipment may differ significantly from previous versions.

---

## ✨ New Features

### RBMK Universal Coolant Expansion

Expanded RBMK and heat exchanger coolant compatibility with 24 new hot fluid variants:

* Hot Mercury
* Hot Estradiol Solution
* Hot Carbon Dioxide
* Hot Hydrogen
* Hot Oxygen
* Hot Xenon Gas
* Hot Kerosene
* Hot Nitroglycerin
* Hot Deuterium
* Hot Tritium
* Hot Hydrogen Peroxide
* Hot Antimatter
* Hot Schrabidic Acid
* Hot Antischrabidium
* Hot Nitric Acid
* Hot Stellar Flux
* Hot Mustard Gas
* Hot Phosgene
* Hot Liquid Nuclear Waste
* Hot NITAN© Super Fuel
* Hot BF Rocket Fuel
* Hot Booster Pheromone
* Hot Sunflower Seed Oil
* Hot Sulfuric Acid

Additional coolant compatibility changes:

* Heavy Water, Liquid Sodium, Liquid Lead, and Liquid Thorium Salt now support the RBMK heat exchanger (`HEATEXCHANGER`) trait alongside their existing PWR/ICF roles.
* Cold Perfluoromethyl now has the RBMK heater trait, completing its heat exchanger loop.
* Added all required heatable/coolable trait pairs, GUI textures, models, and localization entries.

### Nuclear Waste RTG Pellet

Added a new Nuclear Waste RTG Pellet item.

### Type I Compliance Module

Added the Type I Compliance Module.

### Steel Scaffold Variants

Added white, yellow, and red steel scaffold variants.

### Paperclips

Added Paperclips.

---

## ⚙️ Gameplay & Balance Changes

### BAT 9000

* The BAT 9000 can now store antimatter.

---

## 🔧 Technical Changes

### Radiation System

* Added the `IRadShielding` interface.
* Added `ShieldingRegistry` with material-specific HVL values.
* Added the `RadiationOcclusion` system for directional radiation raycasting and dose calculation.
* Converted discrete point sources from legacy `incrementRad()` calls to `RadiationSource` registration.
* Removed the ad-hoc `blocksRad()` shielding check from `TileEntityReactorResearch`.
* Added block-change invalidation events for radiation occlusion calculations.
* Added `ARSTimerSavedData` for persistent radiation death timers.
* Added `CancerSavedData` for persistent cancer burden and monthly dose accumulation.
* Migrated environmental point-source emitters including Watz explosions, Storage Drum overflow, Annihilator destruction, Sellafield blocks, Hazard blocks, and Gas Meltdowns to the new radiation source system.
* Ambient, biome, and fallout radiation continue to use the legacy pocket-diffusion system.

### Radiation Occlusion Performance

* Added cached per-source, per-entity occlusion geometry.
* Added block-boundary invalidation to avoid unnecessary raycast recomputation.

---

## 🛠️ Fixes

* Added microwave sound effects.

---

## 🧹 Miscellaneous