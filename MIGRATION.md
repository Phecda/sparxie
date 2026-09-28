# RNExplorer → Sparxie 原生迁移路线图

> 状态：Draft · 2026-09-24  
> 目标：以 RNExplorer 当前已经接入的功能为参考，在 Sparxie 中分别实现 Android 与 iOS 原生版本。  
> 迁移顺序：按照 `RNExplorer/ROADMAP.md` 的 P0 → P1 顺序逐步实现；P2/P3 不提前纳入当前阶段。
> 迁移方式：Android 与 iOS 是两条独立路线，不要求阶段同步、实现同步或内部协议一致。

## 1. 已确认的迁移边界

- 功能按 `RNExplorer/ROADMAP.md` 的优先级逐步迁移，先完成 P0，再进入 P1。
- P0 只包含基础 Client/Server 功能；P1 不得提前混入 P0 的页面、配置或验收。
- P2/P3 配置不属于当前迁移阶段，除非后续单独调整路线图。
- Android 使用 Jetpack Compose。
- iOS 使用 SwiftUI。
- 两个平台可以使用相同版本的 iperf3 源码，但分别维护各自的构建、native wrapper、线程、取消、错误和事件模型。
- 涉及持久化的数据使用各平台原生存储，不共享文件格式。
- 未开放为用户配置的 iperf3 参数使用 RNExplorer 当前默认值或 native 固定值。
- 当前阶段不实现 Records；Records 作为后续独立阶段重新定义。
- More 保留 Settings 和 Licenses；Settings 的具体内容不在本路线图中扩展。
- 只考虑前台使用，不把后台运行纳入本阶段。
- UI 以功能完整和平台原生体验为验收标准，不要求像素级复刻 RNExplorer。
- 不迁移历史数据。

## 2. 分阶段功能范围

### 2.1 P0 — 基础功能

P0 是第一阶段的最小可用功能，只开放原 `ROADMAP.md` 标为 P0 的字段。

#### Client

- Server Address（必填）
- Server Port（默认 `5201`）
- Parallel Streams（默认 `1`）

P0 中以下行为使用 RNExplorer 当前默认值或固定值，不作为用户配置项：

- Transfer Direction：Upload
- Protocol：TCP
- Duration：使用当前默认时长
- Report Interval：使用当前 native 固定值
- Connect Timeout：使用当前 native 固定值

#### Server

- Server Port（默认 `5201`）
- 监听所有可用地址，不开放 Bind Address 配置
- Server 持续运行，使用停止操作结束，不开放 One-off 配置

#### P0 通用能力

- 启动和停止 Client/Server
- 前台运行状态
- 成功结束和失败结束
- 错误传递和展示所需的基础信息
- 重复启动、重复停止和资源释放语义

### 2.2 P1 — 第一轮扩展

P0 完成并验证后，按照原 `ROADMAP.md` 进入 P1。

#### Client P1

- Transfer Direction：Download、Bidirectional
- Duration
- Protocol：TCP、UDP
- Target Bitrate
- Bind Address
- Connect Timeout
- Report Interval

#### Server P1

- One-off
- Bind Address
- Report Interval
- Server Bitrate Limit
- Max Test Duration

P1 阶段再扩展 native 配置模型、输入校验、页面字段和对应测试；P0 阶段不预先实现这些配置。

### 2.3 P2/P3 — 当前阶段后置

以下内容不进入当前迁移阶段，保留原 `RNExplorer/ROADMAP.md` 的优先级，后续单独决定是否立项：

- P2：Omit、Bytes、Block Count、Buffer Length、Socket Buffer、Client Port、IP Version、TCP No Delay、MSS、Get Server Output、Server Limit Averaging Window、Idle Timeout、Receive Timeout 等。
- P3：SCTP、MPTCP、SSL auth、bind-dev、CPU affinity、TCP congestion、fq-rate、flowlabel、dont-fragment、TCP keepalive、日志文件、daemon、debug 和其他低优先级输出选项。

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

- 选择并固定 Android 端的 iperf3 来源和构建方式。
- 提供后续 P0 Client/Server 所需的 Android native 调用基础。
- 明确 Android 自己的线程、取消、错误和资源释放语义。

**验收**：Android 可构建并加载 iperf3 native 产物，底层调用基础和资源边界明确；本阶段不实现 Client/Server 页面、配置流程或端到端测试。

### A3 — Android Client/Server P0

A3 按可独立验收的垂直切片推进，不按单个页面字段拆分。A3.1 是后续页面接入的前置目标，A3.2/A3.3 分别完成 Server 和 Client 的 P0 闭环，A3.4 负责完整互通和生命周期回归。

#### A3.1 — Android P0 Session Runtime

- 提供后续 Client/Server 页面所需的 Android native 调用入口和 Kotlin session 协调能力。
- 提供 Client/Server P0 配置模型：Client 的 Server Address、Server Port、Parallel Streams；Server 的 Server Port。
- 固定 P0 行为：TCP、Client Upload、默认 Duration；Server 监听所有可用地址。
- 将阻塞的 iperf Client/Server 调用放在专用串行后台执行环境中。
- 保证进程内同一时间最多运行一个 iperf session。
- 明确启动、停止、成功、失败、重复操作和资源释放语义。
- 通过 `iperf_interrupt` 实现取消，由运行线程负责最终释放 `iperf_test`。

**验收**：Android native 层可以验证 P0 Client/Server 的启动、停止、成功、失败和资源释放流程；本目标不接入 Compose 页面，不实现 P1 配置和端到端互通。

#### A3.2 — Android Server P0

- 将 Server 页面接入 Android iperf3 P0 能力。
- 实现 Server Port，默认值为 `5201`。
- 实现 Server 的启动、停止、运行状态、成功、失败和重复操作处理。
- 使用 Android 原生 Compose/Material 控件，不为追求 iOS 样式一致而新增自定义组件。

**验收**：Android Server 可以使用 P0 配置完成前台监听和停止；启动失败、重复操作和停止后的再次启动均有明确结果。

#### A3.3 — Android Client P0

- 将 Client 页面接入 Android iperf3 P0 能力。
- 实现 Server Address、Server Port、Parallel Streams，默认值分别按 P0 约定处理。
- 使用 P0 固定的 Upload、TCP 和默认 Duration 行为。
- 实现输入校验、启动、停止、运行状态、成功、失败和重复操作处理。
- 使用 Android 原生 Compose/Material 控件，不为追求 iOS 样式一致而新增自定义组件。

**验收**：Android Client 可以使用 P0 配置完成前台 TCP Upload 测试，并能正确处理无效地址、连接失败、停止和重复操作。

#### A3.4 — Android P0 互通与生命周期回归

- 验证 Android Client 与 Android Server 的 P0 互通。
- 验证 Android Client 与外部 iperf3 Server 的互通。
- 验证外部 iperf3 Client 与 Android Server 的互通。
- 覆盖运行中停止、失败后重新启动、Client/Server 交替启动和重复操作。
- 覆盖前台页面切换和资源释放，不扩展到后台运行、前台服务或网络切换场景。

**验收**：Android Client/Server 可完成 P0 前台互通，停止、失败、重复启动和资源释放行为不回归。

### A4 — Android Client/Server P1

- 在 P0 Client/Server 验收完成后，接入 P1 配置。
- 扩展 Client 的方向、时长、协议、bitrate、绑定地址、连接超时和报告间隔。
- 扩展 Server 的 One-off、绑定地址、报告间隔、bitrate 限制和最大测试时长。
- 为新增字段补充默认值、输入校验、状态展示和停止行为。

**验收**：Android Client/Server 可完成原 `ROADMAP.md` P1 范围内的前台测试，P0 行为不回归。

### A5 — Android More

- 实现 Settings 页面入口。
- 实现 Licenses 列表和详情入口。
- 使用 Android 原生资源和页面组织方式承载许可证内容。

**验收**：Android 可从 More 进入 Settings 和 Licenses，并能返回一级入口。

### A6 — Android 必要测试

- 覆盖 P0 Client/Server 配置和默认值。
- 覆盖 P0 输入校验、启动、停止、成功、失败和重复操作。
- 覆盖 Android iperf3 集成的关键生命周期。
- P1 完成后补充 P1 配置和回归测试。
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

- 选择并固定 iOS 端的 iperf3 来源和构建方式。
- 提供后续 P0 Client/Server 所需的 iOS native 调用基础。
- 明确 iOS 自己的线程、取消、错误和资源释放语义。

**验收**：iOS 可构建并加载 iperf3 native 产物，底层调用基础和资源边界明确；本阶段不实现 Client/Server 页面、配置流程或端到端测试。

### I3 — iOS Client/Server P0

I3 按可独立验收的垂直切片推进，不按单个页面字段拆分。I3.1 是后续页面接入的前置目标，I3.2/I3.3 分别完成 Server 和 Client 的 P0 闭环，I3.4 负责完整互通和生命周期回归。

#### I3.1 — iOS P0 Session Runtime

- 提供 Client/Server P0 配置模型：Client 的 Server Address、Server Port、Parallel Streams；Server 的 Server Port。
- 固定 P0 行为：TCP、Client Upload、默认 Duration；Server 监听所有可用地址。
- 提供启动、停止、成功、失败和重复操作所需的 native session 协调能力。
- 将阻塞的 iperf Client/Server 调用放在专用串行后台执行环境中。
- 保证进程内同一时间最多运行一个 iperf session。
- 明确 `iperf_test` 的创建、取消、错误传递和释放边界。

**验收**：native 层可以验证 P0 Client/Server 的启动、停止、成功、失败和资源释放流程；本目标不接入 SwiftUI 页面，不实现 P1 配置和端到端互通。

#### I3.2 — iOS Server P0

- 将 Server 页面接入 iOS iperf3 P0 能力。
- 实现 Server Port，默认值为 `5201`。
- 实现 Server 的启动、停止、运行状态、成功、失败和重复操作处理。
- 使用 iOS 原生 SwiftUI 控件，不为追求 Android 样式一致而新增自定义组件。

**验收**：iOS Server 可以使用 P0 配置完成前台监听和停止；启动失败、重复操作和停止后的再次启动均有明确结果。

#### I3.3 — iOS Client P0

- 将 Client 页面接入 iOS iperf3 P0 能力。
- 实现 Server Address、Server Port、Parallel Streams，默认值分别按 P0 约定处理。
- 使用 P0 固定的 Upload、TCP 和默认 Duration 行为。
- 实现输入校验、启动、停止、运行状态、成功、失败和重复操作处理。
- 使用 iOS 原生 SwiftUI 控件，不为追求 Android 样式一致而新增自定义组件。

**验收**：iOS Client 可以使用 P0 配置完成前台 TCP Upload 测试，并能正确处理无效地址、连接失败、停止和重复操作。

#### I3.4 — iOS P0 互通与生命周期回归

- 验证 iOS Client 与 iOS Server 的 P0 互通。
- 验证 iOS Client 与外部 iperf3 Server 的互通。
- 验证外部 iperf3 Client 与 iOS Server 的互通。
- 覆盖运行中停止、失败后重新启动、Client/Server 交替启动和重复操作。
- 覆盖前台页面切换和资源释放，不扩展到后台运行或网络切换场景。

**验收**：iOS Client/Server 可完成 P0 前台互通，停止、失败、重复启动和资源释放行为不回归。

### I4 — iOS Client/Server P1

- 在 P0 Client/Server 验收完成后，接入 P1 配置。
- 扩展 Client 的方向、时长、协议、bitrate、绑定地址、连接超时和报告间隔。
- 扩展 Server 的 One-off、绑定地址、报告间隔、bitrate 限制和最大测试时长。
- 为新增字段补充默认值、输入校验、状态展示和停止行为。

**验收**：iOS Client/Server 可完成原 `ROADMAP.md` P1 范围内的前台测试，P0 行为不回归。

### I5 — iOS More

- 实现 Settings 页面入口。
- 实现 Licenses 列表和详情入口。
- 使用 iOS 原生资源和页面组织方式承载许可证内容。

**验收**：iOS 可从 More 进入 Settings 和 Licenses，并能返回一级入口。

### I6 — iOS 必要测试

- 覆盖 P0 Client/Server 配置和默认值。
- 覆盖 P0 输入校验、启动、停止、成功、失败和重复操作。
- 覆盖 iOS iperf3 集成的关键生命周期。
- P1 完成后补充 P1 配置和回归测试。
- 覆盖 SwiftUI 页面中的主要用户流程。

## 5. 平台独立原则

- Android 与 iOS 可以有不同的页面组织、状态名称、错误模型、事件模型和 iperf3 集成方式。
- 两个平台可以共用同一版本的 iperf3 源码，但分别构建和验证各自的 AAR/XCFramework 或其他 native 产物。
- 任一平台的实现不需要等待另一平台完成。
- 每个平台都按自己的 P0 → P1 顺序推进，不要求两个平台同时进入同一阶段。

## 6. 后续阶段：Records

Records 不在当前阶段实现，后续单独立项并重新定义：

- 是否继续保留历史记录能力；
- 记录保存哪些字段；
- 删除、清空和详情规则；
- 是否需要应用重启恢复；
- Android 与 iOS 是否采用不同的记录模型。

## 7. 本阶段不做

- P2/P3 iperf3 配置，除非后续单独调整路线图。
- Records、记录详情、记录上限和历史记录恢复。
- 后台运行、前台服务、后台限制和网络切换场景。
- RNExplorer 与 Sparxie 之间的历史数据迁移。
- 像素级 UI 复刻。
