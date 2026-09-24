# RNExplorer → Sparxie 原生迁移路线图

> 状态：Draft · 2026-09-24  
> 目标：以 RNExplorer 当前已经接入的功能为参考，在 Sparxie 中分别实现 Android 与 iOS 原生版本。  
> 迁移方式：Android 与 iOS 是两条独立路线，不要求阶段同步、实现同步或内部协议一致。

## 1. 已确认的迁移边界

- 只迁移 RNExplorer 当前已经接入的功能；`RNExplorer/ROADMAP.md` 中尚未接入的 iperf3 高级配置不属于本阶段。
- Android 使用 Jetpack Compose。
- iOS 使用 SwiftUI。
- 两个平台分别选择 iperf3 的来源和集成方式，不要求复用同一份源码、版本或内部实现。
- 涉及持久化的数据使用各平台原生存储，不共享文件格式。
- Client/Server 的默认值沿用 RNExplorer 当前默认值；其他行为允许由各平台重新设计。
- Android 与 iOS 可以分别定义运行状态、错误、事件和数据模型。
- 当前阶段不实现 Records；Records 作为后续独立阶段重新定义。
- More 保留 Settings 和 Licenses；Settings 的具体内容不在本路线图中扩展。
- 只考虑前台使用，不把后台运行纳入本阶段。
- UI 以功能完整和平台原生体验为验收标准，不要求像素级复刻 RNExplorer。
- 不迁移历史数据。

## 2. 当前功能基线

### 2.1 一级入口

- Client
- Server
- More

### 2.2 Client

迁移当前 RNExplorer 已接入的 Client 能力：

- Server Address
- Server Port
- Parallel Streams
- Transfer Direction：Upload、Download、Bidirectional
- Duration
- Protocol：TCP、UDP
- Target Bitrate 与单位
- Bind Address
- Connect Timeout
- Report Interval
- 启动、停止、前台运行状态和错误展示

默认值以 RNExplorer 当前实现为准；具体校验、状态展示和交互由各平台重新设计。

### 2.3 Server

迁移当前 RNExplorer 已接入的 Server 能力：

- Bind Address
- Server Port
- One-off
- Report Interval
- Server Bitrate Limit 与单位
- Max Test Duration
- 启动、停止、前台运行状态和错误展示

默认值以 RNExplorer 当前实现为准；具体校验、状态展示和交互由各平台重新设计。

### 2.4 More

- Settings
- Licenses

Settings 只纳入迁移范围，不在本路线图中新增设置项或预先规定设置模型。

## 3. Android 独立路线

Android 路线不等待 iOS，也不依赖 iOS 的接口或阶段结果。

实现 Client/Server 页面时，如果后续功能和状态逻辑变复杂，再将导航、状态连接和业务协调逻辑拆分到独立的 Route/容器组件，页面组件只接收状态和事件回调。

### A1 — Compose 原生壳

- 建立 Compose 应用入口和主题。
- 建立 Client、Server、More 三个一级入口。
- 建立 Client、Server、More 子页面的导航路径和返回行为。
- 为 Client/Server/More 提供可运行的空页面和基础空状态。

**验收**：Android 可从三个一级入口进入目标页面，页面路径和返回行为可用。

### A2 — Android iperf3 集成

- 选择 Android 端的 iperf3 来源和集成方式。
- 提供 Android 内部可调用的 Client/Server 启动、停止、状态和事件能力。
- 明确 Android 自己的线程、取消、错误和资源释放语义。
- 只实现当前 Client/Server 功能基线所需的 iperf3 参数。

**验收**：Android 可在前台启动和停止 iperf3 Client/Server，成功和失败路径均能结束。

### A3 — Android Client

- 将 Client 配置页面接入 Android iperf3 能力。
- 实现当前功能基线中的字段、默认值、输入、校验和操作。
- 实现 Android 自己的运行状态、错误展示和停止交互。

**验收**：Android Client 可完成前台测试，默认值与 RNExplorer 当前默认值一致。

### A4 — Android Server

- 将 Server 配置页面接入 Android iperf3 能力。
- 实现当前功能基线中的字段、默认值、输入、校验和操作。
- 实现 Android 自己的运行状态、错误展示和停止交互。

**验收**：Android Server 可完成前台测试，默认值与 RNExplorer 当前默认值一致。

### A5 — Android More

- 实现 Settings 页面入口。
- 实现 Licenses 列表和详情入口。
- 使用 Android 原生资源和页面组织方式承载许可证内容。

**验收**：Android 可从 More 进入 Settings 和 Licenses，并能返回一级入口。

### A6 — Android 必要测试

- 覆盖 Client/Server 配置和默认值。
- 覆盖输入校验、启动、停止、成功、失败和重复操作等关键路径。
- 覆盖 Android iperf3 集成的关键生命周期。
- 覆盖 Compose 页面中的主要用户流程。

## 4. iOS 独立路线

iOS 路线不等待 Android，也不依赖 Android 的接口或阶段结果。

### I1 — SwiftUI 原生壳

- 建立 SwiftUI 应用入口和主题。
- 建立 Client、Server、More 三个一级入口。
- 建立 Client、Server、More 子页面的导航路径和返回行为。
- 为 Client/Server/More 提供可运行的空页面和基础空状态。

**验收**：iOS 可从三个一级入口进入目标页面，页面路径和返回行为可用。

### I2 — iOS iperf3 集成

- 选择 iOS 端的 iperf3 来源和集成方式。
- 提供 iOS 内部可调用的 Client/Server 启动、停止、状态和事件能力。
- 明确 iOS 自己的线程、取消、错误和资源释放语义。
- 只实现当前 Client/Server 功能基线所需的 iperf3 参数。

**验收**：iOS 可在前台启动和停止 iperf3 Client/Server，成功和失败路径均能结束。

### I3 — iOS Client

- 将 Client 配置页面接入 iOS iperf3 能力。
- 实现当前功能基线中的字段、默认值、输入、校验和操作。
- 实现 iOS 自己的运行状态、错误展示和停止交互。

**验收**：iOS Client 可完成前台测试，默认值与 RNExplorer 当前默认值一致。

### I4 — iOS Server

- 将 Server 配置页面接入 iOS iperf3 能力。
- 实现当前功能基线中的字段、默认值、输入、校验和操作。
- 实现 iOS 自己的运行状态、错误展示和停止交互。

**验收**：iOS Server 可完成前台测试，默认值与 RNExplorer 当前默认值一致。

### I5 — iOS More

- 实现 Settings 页面入口。
- 实现 Licenses 列表和详情入口。
- 使用 iOS 原生资源和页面组织方式承载许可证内容。

**验收**：iOS 可从 More 进入 Settings 和 Licenses，并能返回一级入口。

### I6 — iOS 必要测试

- 覆盖 Client/Server 配置和默认值。
- 覆盖输入校验、启动、停止、成功、失败和重复操作等关键路径。
- 覆盖 iOS iperf3 集成的关键生命周期。
- 覆盖 SwiftUI 页面中的主要用户流程。

## 5. 平台独立原则

- Android 与 iOS 可以有不同的页面组织、状态名称、错误模型、事件模型和 iperf3 集成方式。
- 任一平台的实现不需要等待另一平台完成。
- 平台内部方案由各自实现阶段决定。

## 6. 后续阶段：Records

Records 不在当前阶段实现，后续单独立项并重新定义：

- 是否继续保留历史记录能力；
- 记录保存哪些字段；
- 删除、清空和详情规则；
- 是否需要应用重启恢复；
- Android 与 iOS 是否采用不同的记录模型。

## 7. 本阶段不做

- RNExplorer `ROADMAP.md` 中未接入的 iperf3 高级配置。
- Records、记录详情、记录上限和历史记录恢复。
- 后台运行、前台服务、后台限制和网络切换场景。
- RNExplorer 与 Sparxie 之间的历史数据迁移。
- 像素级 UI 复刻。
