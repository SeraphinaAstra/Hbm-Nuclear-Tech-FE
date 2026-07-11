# HBM's Nuclear Tech Mod: Final Edition

> [!NOTE]
> NTM FE is a solo passion project. It tracks NTM: Community Edition upstream and stays broadly compatible with its direction, but it is not an official CE fork, not affiliated with the CE team, and not intended as a replacement for it. If you want the mainline experience, go play CE. If you want unhinged reactor coolant options and features CE would (rightly) never ship, you're in the right place.

## What is this?

NTM FE is an actively developed personal fork layered on top of NTM: Community Edition, same foundation, same commitment to feeling like a real industrial/nuclear tech mod, but steered toward the specific features, mechanics, and absurdities I actually want to see in it.

Some additions lean toward realism, some lean toward chaos (yes, you can cool your RBMK with antimatter; no, it will not be practical; yes, that's the point). Both are equally in-scope. The unifying rule is simple: if a limitation in the base mod is arbitrary rather than a real design constraint, it's fair game to remove.

## Notable additions over base NTM

- **RBMK Universal Coolant**,  every fluid in the mod that makes sense as a coolant (and several that absolutely do not) can now be used in the RBMK fluid heater and heat exchanger, not just the small hardcoded default list. Real thermal properties inform the balance where a real fluid exists; vibes inform it where the fluid is antimatter, nitroglycerin, or estradiol solution.
- More reactor and machine work in progress, see open issues/project board for current status.

This list will grow as features land. Check commit history and the issue tracker for the current bleeding edge.

## Is it survival ready?

Mostly, with the understanding that this is a personal fork under active, sometimes chaotic development — expect things to be in flux more often than a stable release branch would be.

## Compatibility

Not guaranteed to be compatible with NTM: Extended Edition addons, other NTM forks, or worlds created on those forks. Treat this as its own thing, not a drop-in replacement for CE or any other fork.

## Development

Built on the same foundation as NTM: Community Edition — see CE's own documentation for the base toolchain (JDK setup, `gradlew` usage, Apple Silicon quirks, etc.), since none of that changes here. This README covers what's specific to FE; refer to upstream CE docs for the general Forge/Gradle workflow.

## Licensing

Licensed under the GNU GPLv3 / LGPLv3, consistent with upstream NTM: Community Edition — see `LICENSE` and `LICENSE.LESSER`.