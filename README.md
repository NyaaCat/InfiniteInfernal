# InfiniteInfernal

A sophisticated Minecraft server plugin that transforms ordinary mobs into powerful, customizable "Infernal" mobs with level-based difficulty scaling, unique abilities, advanced AI, and configurable loot systems.

[中文文档](README.zh.md)

## Features

- **Level-Based Mob Scaling**: Mobs scale with configurable levels affecting health, damage, and rewards
- **33+ Active Abilities**: Combat abilities mobs use against players (meteor, beam, summon, teleport, etc.)
- **12+ Passive Abilities**: Defensive and trigger-based abilities (thorns, armor, resurrection, etc.)
- **Weighted Loot System**: Common and special drops with player skill modifiers
- **Boss Bar Tracking**: Real-time health display for each infernal mob
- **Region-Based Spawning**: Custom mob spawning rules per region
- **Group System**: Cooperative play with configurable loot/exp distribution
- **WorldGuard Integration**: Spawn region validation
- **True Damage System**: Damage types that ignore resistance

## Requirements

- Paper/Spigot 1.21+
- Java 21+
- [NyaaCore](https://github.com/NyaaCat/NyaaCore) (required dependency)

## Installation

1. Download the latest release
2. Place the JAR file in your server's `plugins` folder
3. Ensure NyaaCore is also installed
4. Restart the server
5. Configure the plugin in `plugins/InfiniteInfernal/`

## Configuration

### Directory Structure

```
plugins/InfiniteInfernal/
├── config.yml          # Main configuration
├── mobs/               # Mob definitions
│   ├── zombie-1.yml
│   └── ...
├── abilities/          # Ability set definitions
│   ├── set-generic-spawn.yml
│   └── ...
├── levels/             # Level configurations
│   ├── level-1.yml
│   └── ...
├── regions/            # Region-specific spawning
│   └── ...
└── loots/              # Loot item definitions
    └── ...
```

### Main Configuration (config.yml)

```yaml
language: en_US
enabled: true

# Name tag format for infernal mobs
# Placeholders: {level.prefix}, {mob.name}, {level.level}, {mob.type}
nameTag: '&r[&8Infernal&r] {level.prefix}{mob.name} &cLv.&e{level.level}&r'

# Boss bar settings
bossbar:
  enabled: true
  killsuffix: ' &e[KILLED]'

# Scoreboard tags added to all infernal mobs
tags:
  - im_mob

# World-specific configurations
worlds:
  world:
    enabled: true
    disable-natural-spawning: true
    max-mob-per-player: 10
    max-mob-in-world: 240
    spawn-range-min: 16
    spawn-range-max: 50
    spawn-interval: 20           # Ticks between spawn attempts
    mob-active-interval: 60      # Ticks between ability cycles
    despawnRange: 128

    # Aggro configuration
    aggro:
      range:
        min: 4
        max: 56
      base: 32
      dec: effect:INVISIBILITY:4  # Reduces aggro
      inc: effect:GLOWING:10      # Increases aggro

    # Loot configuration
    looting:
      global: 85                  # Base drop chance %
      overall:
        inc:
          - attribute:GENERIC_LUCK:5
          - enchant:looting:3
        dec:
          - effect:UNLUCK:10
        max: 40.0
```

### Mob Configuration (mobs/*.yml)

```yaml
name: '&7Infernal Zombie'        # Display name (supports color codes)
type: ZOMBIE                      # Bukkit EntityType

# Abilities (references ability sets)
abilities:
  - set-generic-spawn
  - set-attack-poison

# NBT tags to apply (optional)
nbttags: '{ArmorDropChances:[0.0f,0.0f,0.0f,0.0f]}'

# Health override (-1 = use level config)
healthOverride: -1

# Dynamic health calculation
enableDynamicHealth: false
dynamicHealthExpression: 'maxHealth * playerCount * 0.5'

# Spawn configuration
spawn:
  autoSpawn: true                 # Include in natural spawning
  weight: 100                     # Selection weight
  levels:
    - '1-5'                       # Level range
    - '10'                        # Single level
  worlds:
    - world                       # Allowed worlds (empty = all)
  biomes: []                      # Allowed biomes (empty = all)

# Loot configuration
loot:
  vanilla: false                  # Drop vanilla loot
  imLoot: true                    # Drop configured loot
  special:
    chance: 20.0                  # Special drop chance %
    list:
      - rare-sword:50             # item:weight
      - rare-armor:30
  expOverride: -1                 # -1 = use level config
```

### Level Configuration (levels/*.yml)

```yaml
level: 5
prefix: '&e'                      # Color prefix for name

attr:
  health: 100.0
  damage: 15.0
  damageResist: 0.0               # Percentage damage reduction
  exp: 25
  aggro: 32.0                     # Aggro range

spawnConfig:
  from: 500                       # Min distance from world spawn
  to: 1500                        # Max distance from world spawn
  weight: 10                      # Selection weight at this distance
```

### Ability Set Configuration (abilities/*.yml)

```yaml
weight: 10                        # Selection weight for this set

abilities:
  TeleportToPlayer:
    __class__: cat.nyaa.infiniteinfernal.ability.impl.active.AbilityTeleportToPlayer
    radius: 10.0                  # Teleport radius around target
    range: 64.0                   # Max range to target

  SkillEffect:
    __class__: cat.nyaa.infiniteinfernal.ability.impl.active.AbilitySkillEffect
    particle:
      type: PORTAL
      amount: 100
    particleEnabled: true
    soundEnabled: true
    sound: ENTITY_ENDERMAN_TELEPORT
```

### Region Configuration (regions/*.yml)

```yaml
name: dungeon_region

region:
  world: world
  xMin: 100
  xMax: 200
  yMin: 60
  yMax: 150
  zMin: -100
  zMax: 0

mobs:
  - boss-mob:100                  # mob:weight
  - minion-mob:50

followGlobalLevel: false          # Use region-specific levels
maxSpawnAmountOverride: 50        # Max mobs in region
```

## Commands

### Main Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/ii reload` | `im.reload` | Reload all configurations |
| `/ii spawn <mob> [world x y z] [level]` | `im.spawn` | Spawn a mob manually |
| `/ii addloot <name> [dynamic]` | `im.addloot` | Add held item as loot |
| `/ii getloot <name>` | `im.getloot` | Get loot item |
| `/ii setdrop <item> <level> <weight>` | `im.setdrop` | Set loot for level |
| `/ii inspect <level>` | `im.inspect` | View level's loot table |
| `/ii kill all` | `im.kill.all` | Kill all infernal mobs |

**Aliases**: `infiniteinfernal`, `ii`, `im`, `inf`

### Group Commands (`/ig`)

| Command | Description |
|---------|-------------|
| `/ig join <group>` | Join a group |
| `/ig leave` | Leave current group |
| `/ig list` | List group members |
| `/ig kick <player>` | Remove a member |
| `/ig disband` | Disband the group |
| `/ig setlootmode <roll\|round\|equal>` | Set loot distribution |
| `/ig setexpmode <share\|average>` | Set exp distribution |

### Message Control (`/imi`)

| Command | Description |
|---------|-------------|
| `/imi all` | Receive all kill messages |
| `/imi me` | Only your kills |
| `/imi near` | Nearby kills only |
| `/imi off` | Disable messages |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `im.command` | true | Use basic commands |
| `im.reload` | op | Reload configurations |
| `im.spawn` | op | Spawn mobs manually |
| `im.addloot` | op | Add loot items |
| `im.getloot` | op | Retrieve loot items |
| `im.setdrop` | op | Configure drops |
| `im.inspect` | op | View loot tables |
| `im.kill.all` | op | Kill all mobs |
| `im.debug` | op | Debug commands |
| `im.group` | true | Group commands |
| `imi.command` | true | Message control |

## Ability Types

### Active Abilities (Combat)

| Ability | Description |
|---------|-------------|
| `AbilityBeam` | Damaging beam projectile |
| `AbilityMeteor` | Meteors fall from sky |
| `AbilityFang` | Evoker fang attack |
| `AbilitySummon` | Summon minion mobs |
| `AbilityClone` | Create mob clones |
| `AbilityProjectile` | Throw projectiles |
| `AbilityTeleportToPlayer` | Teleport near target |
| `AbilityLifesteal` | Steal health on hit |
| `AbilityFire` | Fire-based attacks |
| `AbilityAOE` | Area of effect damage |
| `AbilityThrowPlayer` | Throw target player |
| `AbilityPull` | Pull target toward mob |
| `AbilityShield` | Temporary invulnerability |
| `AbilityExplode` | Explosion attack |

### Passive Abilities (Defense/Trigger)

| Ability | Description |
|---------|-------------|
| `AbilityArmor` | Increased armor rating |
| `AbilityResistance` | Damage resistance |
| `AbilityThorns` | Reflect damage |
| `AbilityImmunity` | Immune to effect/damage type |
| `AbilityNirvana` | Resurrect on death |
| `AbilityIgnite` | Set attacker on fire |
| `AbilityPotionHit` | Apply potion on attack |

## Dynamic Health Expression

Mobs can have dynamic health calculated from expressions:

**Available Variables:**
- `health` - Base health from level config
- `maxHealth` - Current max health
- `playerCount` - Players near the mob
- `aggroRange` - Mob's aggro range

**Example:**
```yaml
enableDynamicHealth: true
dynamicHealthExpression: 'maxHealth * playerCount * (0.6 + 0.1 * playerCount)'
```

## Loot Modifier Format

Modifiers affect loot drop chances based on player attributes/effects:

```yaml
# Attribute modifier: attribute:ATTRIBUTE_NAME:value
- attribute:GENERIC_LUCK:5

# Effect modifier: effect:EFFECT_NAME:minLevel
- effect:INVISIBILITY:2

# Enchantment modifier: enchant:ENCHANTMENT:level
- enchant:looting:3
```

## Events

The plugin fires custom events that other plugins can listen to:

- `InfernalSpawnEvent` - When an infernal mob spawns
- `LootDropEvent` - When loot is about to drop (cancellable)
- `IMobNearDeathEvent` - When mob is about to die (can prevent death)

## Troubleshooting

### Mobs not spawning
1. Check `enabled: true` in config.yml
2. Verify world is in the `worlds` section
3. Check `autoSpawn: true` in mob config
4. Verify level configs exist for spawn distance
5. Check `max-mob-per-player` and `max-mob-in-world` limits

### Abilities not firing
1. Verify ability set exists in `abilities/` folder
2. Check mob config references correct ability set name
3. Ensure player is within aggro range
4. Check `mob-active-interval` timing

### Loot not dropping
1. Check `imLoot: true` in mob config
2. Verify loot items exist
3. Check `looting.global` percentage in world config
4. Review modifier conditions (luck, effects)

## License

This project is licensed under the MIT License.

## Credits

- NyaaCat Development Team
- Contributors and testers
