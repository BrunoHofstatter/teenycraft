# Ability Spreadsheet Prep

## Legend

### Tab 1 fields
1 = `id`
2 = `name`
3 = `folder`
4 = `description`
5 = `golden_description`
6 = `texture_index`
7 = `particle_compact`
8 = `icon_name`

`particle_compact` format:
- `particle_id - count`
- example: `minecraft:cloud - 0`
- if no particle, use `none`

### Tab 2 fields
1 = `id`
2 = `hit_type`
3 = `damage_tier`
4 = `range_tier`
5 = `raycast_delay_tier`
6 = `effects_on_self`
7 = `effects_on_opponent`
8 = `traits`
9 = `golden_bonus`

Formatting rules for list-like fields:
- Use `none` if empty
- Separate multiple entries with ` ; `
- Effect format: `effect_id(param1,param2)`
- Trait format: `trait_id(param1,param2)`
- Golden bonus format: keep the original authored string exactly
- If an effect/trait has no params, write `effect_id()` or `trait_id()`

## Tab 1

```text
1|2|3|4|5|6|7|8
amazonian_beatdown|Amazonian Beatdown|melee|none|none|161|minecraft:sweep_attack - 0|minecraft.rabbit_foot
bang|Bang!|melee|none|none|132|minecraft:explosion - 3|minecraft.tnt
harleys_mallet|Harley's Mallet|melee|none|none|130|minecraft:crit - 8|minecraft.golden_axe
jiu_jitsu|Jiu Jitsu|melee|none|none|102|minecraft:sweep_attack - 3|minecraft.warped_button
jokers_mallet|Joker's Mallet|melee|none|none|142|minecraft:crit - 8|minecraft.golden_axe
mighty_punch|Mighty Punch|melee|none|none|11|minecraft:crit - 5|minecraft.oak_button
ponder|Ponder|melee|none|none|165|minecraft:crit - 0|minecraft.stick
quick_punch|Quick Punch|melee|none|none|152|minecraft:sweep_attack - 2|minecraft.oak_button
soul_punch|Soul Punch|melee|none|none|14|minecraft:crit - 5|minecraft.polished_blackstone_button
staff_slam|Staff Slam|melee|none|none|15|minecraft:crit - 5|ability_staff_slam
super_sockem|Super Sock'em|melee|none|none|111|minecraft:explosion - 1|minecraft.crimson_button
bat_mine|Bat-Mine|opp_effect|none|none|100|minecraft:smoke - 8|minecraft.tripwire_hook
chattering_teeth|Chattering Teeth|opp_effect|none|none|140|minecraft:crit - 5|minecraft.gold_nugget
curse|Curse|opp_effect|none|none|6|minecraft:dragon_breath - 5|minecraft.popped_chorus_fruit
raspberry|Raspberry|opp_effect|none|none|150|minecraft:spit - 10|minecraft.sweet_berries
riddle_me_this|Riddle Me This|opp_effect|none|none|164|minecraft:electrical_spark - 0|minecraft.redstone
waffle|Waffle|opp_effect|none|none|20|minecraft:dragon_breath - 5|wrapper
bar_deplete|Curse|pure_effects|none|none|6|minecraft:dragon_breath - 5|wrapper
charge_up|Heroic Pose|pure_effects|none|none|9|minecr aft:happy_villager - 5|wrapper
cleanse|Cleanse|pure_effects|none|none|22|minecraft:happy_villager - 5|wrapper
dance_plus|Dance|pure_effects|none|none|8|minecraft:note - 5|wrapper
defense_down|Curse|pure_effects|none|none|6|minecraft:dragon_breath - 5|wrapper
defense_up|Heroic Pose|pure_effects|none|none|9|minecraft:happy_villager - 5|wrapper
dodge_smoke|Heroic Pose|pure_effects|none|none|9|minecraft:happy_villager - 5|wrapper
freeze|Freeze|pure_effects|none|none|25|minecraft:dragon_breath - 5|wrapper
heal|Heal|pure_effects|none|none|0|minecraft:happy_villager - 5|wrapper
health_radio|Health Radio|pure_effects|none|none|16|minecraft:heart - 8|wrapper
kiss|Kiss|pure_effects|none|none|27|minecraft:dragon_breath - 5|wrapper
luck_up|Cuteness|pure_effects|none|none|7|minecraft:cherry_leaves - 5|wrapper
multi_hit|Multi Hit|pure_effects|none|none|15|minecraft:dragon_breath - 5|wrapper
pet|Cuteness|pure_effects|none|none|7|minecraft:cherry_leaves - 50|wrapper
poison|Poison|pure_effects|none|none|15|minecraft:dragon_breath - 5|wrapper
power_down|Curse|pure_effects|none|none|6|minecraft:dragon_breath - 5|wrapper
power_radio|Power Radio|pure_effects|none|none|17|minecraft:enchanted_hit - 8|wrapper
reflect|Battery Drain|pure_effects|none|none|1|minecraft:electric_spark - 5|wrapper
remote_mine|Curse|pure_effects|none|none|2|minecraft:dragon_breath - 5|wrapper
root|Curse|pure_effects|none|none|6|minecraft:dragon_breath - 5|wrapper
shock|Curse|pure_effects|none|none|15|minecraft:dragon_breath - 5|wrapper
around_the_world|Around the World|ranged|none|none|162|minecraft:cloud - 0|minecraft.clock
birdarang|Birdarang|ranged|none|none|2|minecraft:poof - 5|ability_birdarang
black_hole|Black Hole|ranged|none|none|3|minecraft:portal - 5|minecraft.music_disc_11
boulder_toss|Boulder Toss|ranged|none|none|167|minecraft:poof - 0|minecraft.cobblestone
burp_surprise|Burp Surprise|ranged|none|none|5|minecraft:bubble_pop - 5|minecraft.explorer_pottery_sherd
grappling_hook|Grappling Hook|ranged|none|none|101|minecraft:crit - 5|minecraft.iron_hoe
heat_vision|Heat Vision|ranged|none|none|112|minecraft:flame - 10|minecraft.ender_eye
laser_eyes|Laser Eyes|ranged|none|none|10|minecraft:witch - 5|minecraft.ender_eye
laser_eyes2|Laser Eyes|ranged|none|none|121|minecraft:glow - 10|minecraft.ender_eye
missile_barrage|Missile Barrage|ranged|none|none|12|minecraft:explosion - 5|minecraft.firework_rocket
plasma_shot|Plasma Shot|ranged|none|none|13|minecraft:flame - 5|ability_plasma_shot
puddin_pucker|Puddin' Pucker|ranged|none|none|131|minecraft:heart - 5|minecraft.fermented_spider_eye
punchies|Punchies|ranged|none|none|151|minecraft:crit - 5|minecraft.rabbit_foot
shockwave_stomp|Shockwave Stomp|ranged|none|none|166|minecraft:poof - 0|minecraft.iron_boots
whale_drop|Whale Drop|ranged|none|none|18|minecraft:splash - 5|minecraft.pufferfish
battery_drain|Battery Drain|self_effect|none|none|1|minecraft:electric_spark - 5|ability_battery_drain
burp_shield|Burp Shield|self_effect|none|none|4|minecraft:bubble - 5|minecraft.heart_of_the_sea
counterattack|Counterattack|self_effect|none|none|160|minecraft:crit - 0|minecraft.shield
cuteness|Cuteness|self_effect|none|none|7|minecraft:cherry_leaves - 5|minecraft.pink_dye
dance|Dance|self_effect|none|none|8|minecraft:note - 5|minecraft.music_disc_mall
evil_laugh|Evil Laugh|self_effect|none|none|141|minecraft:note - 10|minecraft.torchflower
flight|Flight|self_effect|none|none|110|minecraft:cloud - 5|minecraft.elytra
good_luck|Good Luck|self_effect|none|none|163|minecraft:happy_villager - 0|minecraft.small_dripleaf
heroic_pose|Heroic Pose|self_effect|none|none|9|minecraft:happy_villager - 5|ability_heroic_pose
nuh_uh|Nuh Uh!|self_effect|none|none|120|minecraft:firework - 5|minecraft.feather
the_heal|The Heal|self_effect|none|none|16|minecraft:heart - 5|minecraft.torchflower
tofu|Tofu|self_effect|none|none|17|minecraft:spore_blossom_air - 5|tofu_txt
```

## Tab 2

```text
1|2|3|4|5|6|7|8|9
amazonian_beatdown|melee|11|4|0|none|none|multi_hit(3.0)|trait:undodgeable
bang|melee|10|4|0|self_damage(0.5)|none|none|self:power_up:0.3
harleys_mallet|melee|6|4|0|none|stun(1.0)|none|opponent:bar_deplete:1.0
jiu_jitsu|melee|8|4|0|none|none|multi_hit(3.0) ; group_damage()|trait:undodgeable
jokers_mallet|melee|5|4|0|none|stun(1.0)|none|opponent:stun:1.3
mighty_punch|melee|10|4|0|none|none|activate(2.0,1.0)|trait:tofu_chance:1.5,1.2
ponder|melee|11|4|0|none|none|none|trait:undodgeable
quick_punch|melee|6|4|0|none|stun(0.7)|none|trait:undodgeable
soul_punch|melee|9|4|0|none|none|none|opponent:stun:0.6
staff_slam|melee|6|4|0|none|stun(1.0)|none|opponent:stun:1.2
super_sockem|melee|9|4|0|none|none|none|trait:undodgeable
bat_mine|ranged|8|8|0|none|remote_mine(1.0)|none|opponent:remote_mine:1.3
chattering_teeth|raycasting|8|6|0|none|poison(1.0)|charge_up(1.0)|opponent:poison:1.3
curse|raycasting|0|8|0|none|curse(1.0)|charge_up(1.5)|trait:instant_cast_chance
raspberry|raycasting|0|8|0|none|stun(2.0) ; power_down(2.0)|none|self:waffle_chance:0.5
riddle_me_this|ranged|0|4|0|none|shock(0.5,0.5) ; defense_down(0.5) ; root(0.5)|charge_up(1.4)|trait:instant_cast_chance
waffle|raycasting|0|8|0|none|waffle(1.0,1.0)|undodgeable()|trait:instant_cast
bar_deplete|raycasting|0|8|0|none|bar_deplete(1.0)|none|trait:instant_cast
charge_up|none|7|4|0|power_up(1.0)|none|charge_up(1.0)|self:power_up:0.3
cleanse|none|7|4|0|cleanse(1.0)|none|none|self:power_up:0.3
dance_plus|none|7|4|0|dance(20.0)|none|none|self:dance:1.3
defense_down|raycasting|0|8|0|none|defense_down(1.0)|none|trait:instant_cast
defense_up|none|7|4|0|defense_up(1.0,1.0)|none|none|self:power_up:0.3
dodge_smoke|none|0|4|0|dodge_smoke(1.0,1.0)|none|none|self:power_up:0.3
freeze|raycasting|0|8|0|none|freeze(1.0,1.0)|undodgeable()|trait:instant_cast
heal|none|7|4|0|heal(1.0)|none|none|self:heal:0.3
health_radio|none|7|4|0|health_radio(1.0,1.0)|none|none|self:health_radio:1.0,0.3
kiss|raycasting|0|8|0|none|kiss(1.0)|undodgeable()|trait:instant_cast
luck_up|none|7|4|0|luck_up(1.0,1.0)|none|none|self:cuteness:1.2
multi_hit|raycasting|7|8|0|none|none|multi_hit(4.0)|trait:instant_cast
pet|none|7|4|0|pets(1.0,1.0)|none|none|self:cuteness:1.2
poison|raycasting|7|8|0|none|poison(1.0,1.0)|none|trait:instant_cast
power_down|raycasting|0|8|0|none|power_down(1.0)|none|trait:instant_cast
power_radio|none|7|4|0|power_radio(1.0,1.0)|none|none|self:power_radio:1.0,0.3
reflect|none|7|4|0|reflect(1.0)|none|none|self:power_up:0.3
remote_mine|raycasting|7|8|0|none|remote_mine(1.0)|none|trait:instant_cast
root|raycasting|0|8|0|none|root(1.0)|none|trait:instant_cast
shock|raycasting|0|8|0|none|shock(1.0,1.0)|none|trait:instant_cast
around_the_world|raycasting|7|8|0|none|none|multi_hit(4.0) ; group_damage()|opponent:bar_deplete:1.0
birdarang|raycasting|8|4|7|none|none|none|trait:undodgeable
black_hole|raycasting|6|4|5|none|disable(1.0)|none|opponent:disable:999.0
boulder_toss|raycasting|9|3|6|none|none|group_damage()|opponent:bar_deplete:1.0
burp_surprise|raycasting|9|4|8|none|none|surprise()|trait:tofu_chance:1.5,1.2
grappling_hook|raycasting|5|6|7|none|defense_down(1.0)|none|opponent:defense_down:1.3
heat_vision|raycasting|8|8|0|none|none|blue(1.0)|trait:blue:0.7
laser_eyes|raycasting|9|4|0|none|none|charge_up(1.2)|trait:instant_cast_chance
laser_eyes2|raycasting|9|6|0|none|none|charge_up(1.0)|trait:instant_cast_chance
missile_barrage|raycasting|8|4|6|none|none|group_damage()|opponent:bar_deplete:1.0
plasma_shot|raycasting|9|4|3|none|none|none|opponent:waffle_chance:0.5,1.0
puddin_pucker|raycasting|4|4|7|none|kiss(1.0)|none|self:heal:0.3
punchies|raycasting|9|2|5|none|none|multi_hit(4.0)|trait:undodgeable
shockwave_stomp|raycasting|5|3|5|none|stun(1.2)|none|trait:tofu_chance:1.5,1.2
whale_drop|raycasting|8|4|6|none|none|group_damage()|opponent:stun:0.6
battery_drain|none|7|4|0|self_shock(1.0) ; bar_fill(1.0)|none|none|self:power_up:0.3
burp_shield|none|7|4|0|shield(1.0)|none|none|self:shield:1.2
counterattack|none|0|4|0|reflect(0.7,1.3)|none|none|self:reflect:1.1,1.3
cuteness|none|7|4|0|cuteness(1.0,1.0)|none|none|self:cuteness:1.2
dance|none|7|4|0|dance(1.0)|none|none|self:dance:1.3
evil_laugh|none|7|4|0|group_heal(1.3)|none|charge_up(0.5)|self:group_heal:0.4
flight|none|0|4|0|flight(1.0)|none|none|self:flight:1.3
good_luck|none|0|4|0|luck_up(1.0,1.0)|none|none|self:luck_up:1.0,1.4
heroic_pose|none|7|4|0|power_up(1.0)|none|none|self:power_up:0.3
nuh_uh|none|0|4|0|cleanse(1.0)|none|none|self:cleanse:1.3
the_heal|none|7|4|0|group_heal(1.3)|none|none|self:group_heal:0.4
tofu|none|7|4|0|tofu_spawn(1.0)|none|none|self:power_up:0.35
```
