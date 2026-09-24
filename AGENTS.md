# AGENTS.md

## Git 提交规范

- 提交信息必须使用 Conventional Commits 格式：`<type>(<scope>): <description>`。
- `scope` 可选，`description` 使用简洁的祈使句，不以句号结尾。
- 常用 `type`：`feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`build`、`ci`、`chore`、`revert`。

## 提交操作要求

- 用户明确要求提交时，采用最小流程：确认状态、暂存本次要求的改动、按规范提交、确认提交结果。
- 不因提交请求重复审查 `.gitignore`、遍历无关目录，或运行未要求的构建、测试和其他检查。
- 提交后只需确认提交成功和工作区状态；不影响提交的提示不应反复检查或阻塞提交，在最终回复中简要说明即可。
