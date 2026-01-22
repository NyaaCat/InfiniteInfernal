# InfiniteInfernal

一个功能强大的 Minecraft 服务器插件，可将普通生物转变为具有等级难度缩放、独特技能、高级AI和可配置掉落系统的强大"炼狱"怪物。

[English Documentation](README.md)

## 功能特性

- **等级缩放系统**：怪物根据等级配置调整生命值、伤害和奖励
- **33+ 主动技能**：怪物在战斗中使用的技能（流星、光束、召唤、传送等）
- **12+ 被动技能**：防御和触发类技能（荆棘、护甲、复活等）
- **加权掉落系统**：普通和特殊掉落，支持玩家属性修正
- **Boss血条显示**：实时显示每个炼狱怪物的生命值
- **区域刷怪**：按区域自定义怪物生成规则
- **组队系统**：支持组队游玩，可配置掉落/经验分配方式
- **WorldGuard 集成**：生成区域验证
- **真实伤害系统**：无视抗性的伤害类型

## 环境要求

- Paper/Spigot 1.21+
- Java 21+
- [NyaaCore](https://github.com/NyaaCat/NyaaCore)（必需依赖）

## 安装方法

1. 下载最新版本
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 确保已安装 NyaaCore
4. 重启服务器
5. 在 `plugins/InfiniteInfernal/` 中配置插件

## 配置说明

### 目录结构

```
plugins/InfiniteInfernal/
├── config.yml          # 主配置文件
├── mobs/               # 怪物定义
│   ├── zombie-1.yml
│   └── ...
├── abilities/          # 技能组定义
│   ├── set-generic-spawn.yml
│   └── ...
├── levels/             # 等级配置
│   ├── level-1.yml
│   └── ...
├── regions/            # 区域刷怪配置
│   └── ...
└── loots/              # 掉落物定义
    └── ...
```

### 主配置文件 (config.yml)

```yaml
language: zh_CN
enabled: true

# 炼狱怪物名称标签格式
# 占位符: {level.prefix}, {mob.name}, {level.level}, {mob.type}
nameTag: '&r[&8黑化&r] {level.prefix}{mob.name} &cLv.&e{level.level}&r'

# Boss血条设置
bossbar:
  enabled: true
  killsuffix: ' &e[已击杀]'

# 添加到所有炼狱怪物的计分板标签
tags:
  - im_mob

# 世界配置
worlds:
  world:
    enabled: true
    disable-natural-spawning: true
    max-mob-per-player: 10
    max-mob-in-world: 240
    spawn-range-min: 16
    spawn-range-max: 50
    spawn-interval: 20           # 刷怪间隔（刻）
    mob-active-interval: 60      # 技能循环间隔（刻）
    despawnRange: 128

    # 仇恨配置
    aggro:
      range:
        min: 4
        max: 56
      base: 32
      dec: effect:INVISIBILITY:4  # 降低仇恨
      inc: effect:GLOWING:10      # 增加仇恨

    # 掉落配置
    looting:
      global: 85                  # 基础掉落概率 %
      overall:
        inc:
          - attribute:GENERIC_LUCK:5
          - enchant:looting:3
        dec:
          - effect:UNLUCK:10
        max: 40.0
```

### 怪物配置 (mobs/*.yml)

```yaml
name: '&7下级僵尸'                # 显示名称（支持颜色代码）
type: ZOMBIE                      # Bukkit 实体类型

# 技能（引用技能组）
abilities:
  - set-generic-spawn
  - set-attack-poison

# NBT 标签（可选）
nbttags: '{ArmorDropChances:[0.0f,0.0f,0.0f,0.0f]}'

# 生命值覆盖（-1 = 使用等级配置）
healthOverride: -1

# 动态生命值计算
enableDynamicHealth: false
dynamicHealthExpression: 'maxHealth * playerCount * 0.5'

# 生成配置
spawn:
  autoSpawn: true                 # 参与自然刷怪
  weight: 100                     # 选择权重
  levels:
    - '1-5'                       # 等级范围
    - '10'                        # 单个等级
  worlds:
    - world                       # 允许的世界（空 = 全部）
  biomes: []                      # 允许的生物群系（空 = 全部）

# 掉落配置
loot:
  vanilla: false                  # 掉落原版战利品
  imLoot: true                    # 掉落配置的战利品
  special:
    chance: 20.0                  # 特殊掉落概率 %
    list:
      - rare-sword:50             # 物品:权重
      - rare-armor:30
  expOverride: -1                 # -1 = 使用等级配置
```

### 等级配置 (levels/*.yml)

```yaml
level: 5
prefix: '&e'                      # 名称颜色前缀

attr:
  health: 100.0
  damage: 15.0
  damageResist: 0.0               # 伤害减免百分比
  exp: 25
  aggro: 32.0                     # 仇恨范围

spawnConfig:
  from: 500                       # 距世界出生点最小距离
  to: 1500                        # 距世界出生点最大距离
  weight: 10                      # 在该距离的选择权重
```

### 技能组配置 (abilities/*.yml)

```yaml
weight: 10                        # 该技能组的选择权重

abilities:
  TeleportToPlayer:
    __class__: cat.nyaa.infiniteinfernal.ability.impl.active.AbilityTeleportToPlayer
    radius: 10.0                  # 传送到目标周围的半径
    range: 64.0                   # 最大目标距离

  SkillEffect:
    __class__: cat.nyaa.infiniteinfernal.ability.impl.active.AbilitySkillEffect
    particle:
      type: PORTAL
      amount: 100
    particleEnabled: true
    soundEnabled: true
    sound: ENTITY_ENDERMAN_TELEPORT
```

### 区域配置 (regions/*.yml)

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
  - boss-mob:100                  # 怪物:权重
  - minion-mob:50

followGlobalLevel: false          # 使用区域特定等级
maxSpawnAmountOverride: 50        # 区域最大怪物数量
```

## 命令

### 主命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/ii reload` | `im.reload` | 重载所有配置 |
| `/ii spawn <怪物> [世界 x y z] [等级]` | `im.spawn` | 手动生成怪物 |
| `/ii addloot <名称> [dynamic]` | `im.addloot` | 将手持物品添加为掉落物 |
| `/ii getloot <名称>` | `im.getloot` | 获取掉落物 |
| `/ii setdrop <物品> <等级> <权重>` | `im.setdrop` | 设置等级掉落 |
| `/ii inspect <等级>` | `im.inspect` | 查看等级掉落表 |
| `/ii kill all` | `im.kill.all` | 杀死所有炼狱怪物 |

**别名**: `infiniteinfernal`, `ii`, `im`, `inf`

### 组队命令 (`/ig`)

| 命令 | 说明 |
|------|------|
| `/ig join <组名>` | 加入队伍 |
| `/ig leave` | 离开当前队伍 |
| `/ig list` | 列出队员 |
| `/ig kick <玩家>` | 踢出队员 |
| `/ig disband` | 解散队伍 |
| `/ig setlootmode <roll\|round\|equal>` | 设置掉落分配模式 |
| `/ig setexpmode <share\|average>` | 设置经验分配模式 |

### 消息控制 (`/imi`)

| 命令 | 说明 |
|------|------|
| `/imi all` | 接收所有击杀消息 |
| `/imi me` | 只接收自己的击杀 |
| `/imi near` | 只接收附近的击杀 |
| `/imi off` | 关闭消息 |

## 权限

| 权限 | 默认 | 说明 |
|------|------|------|
| `im.command` | true | 使用基本命令 |
| `im.reload` | op | 重载配置 |
| `im.spawn` | op | 手动生成怪物 |
| `im.addloot` | op | 添加掉落物 |
| `im.getloot` | op | 获取掉落物 |
| `im.setdrop` | op | 配置掉落 |
| `im.inspect` | op | 查看掉落表 |
| `im.kill.all` | op | 杀死所有怪物 |
| `im.debug` | op | 调试命令 |
| `im.group` | true | 组队命令 |
| `imi.command` | true | 消息控制 |

## 技能类型

### 主动技能（战斗）

| 技能 | 说明 |
|------|------|
| `AbilityBeam` | 伤害光束投射 |
| `AbilityMeteor` | 天降陨石 |
| `AbilityFang` | 唤魔者尖牙攻击 |
| `AbilitySummon` | 召唤小怪 |
| `AbilityClone` | 创建分身 |
| `AbilityProjectile` | 投掷物攻击 |
| `AbilityTeleportToPlayer` | 传送到目标附近 |
| `AbilityLifesteal` | 攻击吸血 |
| `AbilityFire` | 火焰攻击 |
| `AbilityAOE` | 范围伤害 |
| `AbilityThrowPlayer` | 投掷目标玩家 |
| `AbilityPull` | 将目标拉向怪物 |
| `AbilityShield` | 临时无敌 |
| `AbilityExplode` | 爆炸攻击 |

### 被动技能（防御/触发）

| 技能 | 说明 |
|------|------|
| `AbilityArmor` | 增加护甲值 |
| `AbilityResistance` | 伤害抗性 |
| `AbilityThorns` | 反弹伤害 |
| `AbilityImmunity` | 免疫效果/伤害类型 |
| `AbilityNirvana` | 死亡后复活 |
| `AbilityIgnite` | 点燃攻击者 |
| `AbilityPotionHit` | 攻击时施加药水效果 |

## 动态生命值表达式

怪物可以使用表达式计算动态生命值：

**可用变量：**
- `health` - 等级配置的基础生命值
- `maxHealth` - 当前最大生命值
- `playerCount` - 怪物附近的玩家数量
- `aggroRange` - 怪物的仇恨范围

**示例：**
```yaml
enableDynamicHealth: true
dynamicHealthExpression: 'maxHealth * playerCount * (0.6 + 0.1 * playerCount)'
```

## 掉落修正格式

修正器根据玩家属性/效果影响掉落概率：

```yaml
# 属性修正: attribute:属性名:值
- attribute:GENERIC_LUCK:5

# 效果修正: effect:效果名:最小等级
- effect:INVISIBILITY:2

# 附魔修正: enchant:附魔名:等级
- enchant:looting:3
```

## 事件

插件触发的自定义事件供其他插件监听：

- `InfernalSpawnEvent` - 炼狱怪物生成时触发
- `LootDropEvent` - 掉落物即将生成时触发（可取消）
- `IMobNearDeathEvent` - 怪物即将死亡时触发（可阻止死亡）

## 故障排除

### 怪物不生成
1. 检查 config.yml 中 `enabled: true`
2. 确认世界在 `worlds` 配置中
3. 检查怪物配置中 `autoSpawn: true`
4. 确认存在对应生成距离的等级配置
5. 检查 `max-mob-per-player` 和 `max-mob-in-world` 限制

### 技能不释放
1. 确认技能组存在于 `abilities/` 文件夹
2. 检查怪物配置引用的技能组名称正确
3. 确保玩家在仇恨范围内
4. 检查 `mob-active-interval` 设置

### 掉落物不掉落
1. 检查怪物配置中 `imLoot: true`
2. 确认掉落物已配置
3. 检查世界配置中 `looting.global` 百分比
4. 检查修正条件（幸运值、效果等）

## 许可证

本项目使用 MIT 许可证。

## 致谢

- 喵窝开发团队
- 贡献者和测试人员
