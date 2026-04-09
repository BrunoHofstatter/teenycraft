# Chips

## Purpose
Hold the design for chips as a dedicated progression/customization system without mixing it into unrelated docs.

## Current Status
Chip-related concepts are referenced in high-level planning, but this should be treated as a planning-space document until the system has concrete runtime implementation.

## Player-Facing Behavior
- Intended role: add another layer of figure customization, build specialization, or utility bonuses.

## Source Of Truth
- No dedicated chip runtime implementation is documented yet.
- When chip code is added, this file should link the exact source paths.

## Design Notes
- Decide early whether chips belong to figures, the Titan Manager, a crafting station, or another progression surface.
- Avoid overlapping chips too much with accessories or stat upgrades; each system should have a distinct purpose.

## Open Questions
- equip location
- permanence and removal rules
- randomness versus deterministic crafting
- whether chips modify figures permanently or only while slotted

## Planned Additions
- chip categories
- acquisition loop
- UI/inventory rules
- technical implementation references once the feature exists
