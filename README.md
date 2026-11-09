# Create: Fueling Imagination

Minecraft 1.20.1 · Forge 47.4.23 · Create 6.0.8 附属模组

给机械动力加上"损坏—维修—改装"的一整套循环，外加放映机、命令方块插件、动力集成工具等内容。

## 主要内容

| 系统 | 说明 |
|---|---|
| 损坏与维修 | 被爆炸打坏的方块可以在动力工作站拆解、清洁、换件、重组；也可用动力拆解器粉碎回收 |
| 背罐 / 喷气背包改装 | 护甲槽 + 升级组件 + 鞘翅槽，曲柄开槽、拉杆重建 |
| 动力集成工具 | 材质槽 + 4 个配件槽，耗压缩空气，7 种配件各有效果 |
| 命令方块-携带版 | 1 个插口槽 + 8 件具象指令插件 |
| 放映机 | 单方块，`MODE` 属性切换全息 / 影画两种投射方式 |
| 前辈的笔记 | 手持手册 shift 右键放下，右键翻开合上，打掉还原手册 |
| 装甲模式 | 30 秒装甲态，速度/跳跃/潮涌加成，举臂瞄准姿势，长按 shift 3 秒退出 |

## 依赖

- Minecraft 1.20.1
- Forge 47.4.23+
- Create 6.0.8-289
- 可选：create_jetpack 4.4.6（喷气背包改装）

## 编译

需要 JDK 17：

```
./gradlew build
```

产物在 `build/libs/create-fueling-imagination-<ver>.jar`。

## 目录

```
src/main/java/com/fuelingimagination/   代码
  content/   玩法逻辑（损坏、工作站、改装、放映机、装甲）
  block/     方块与方块实体
  item/      物品
  client/    界面、渲染器
  mixin/     Mixin
src/main/resources/
  assets/    模型、贴图、语言
  data/      配方、战利品表、标签、进度
tools/       Blockbench 模型转换脚本
docs/        设计与技术笔记
```

## 版本

版本号只递增末位，每次构建都是新版本。