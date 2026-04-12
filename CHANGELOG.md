## Changelog

All notable changes to this project are documented in this release.

## [4.0.1] - 2026-02-25

### 🚀 Features

- Memory fragments can be re locked
- Memory fragments can be created locked or unlocked
- Integrated OpenWebUI to allow usage with knowledge bases

### 🐛 Bug Fixes

- Fix nearby block cache snapshots to prevent stale/unbounded growth and cross-thread mutation risks
- Fix conversation persistence to avoid new duplicate rows and restore chronological history order
- Register NPC death listener once during service initialization and delete config files on delete-by-type
- Cancel button in edit gui now leads back to the main screen

### 💼 Other

- Improve NPC event queue/error handling and normalize config checkbox state updates
- Harden SQLite access paths with fail-fast connection checks and safer prepared-statement usage
- Encapsulate conversation cache behind `ResourceProvider` methods and make storage thread-safe
- Remove unused duplicate/dead code (`NPCUnlockCommand`, `Goal`, `GoalThread`, and unused listener helper)
- Move config-screen UI label constants out of `BaseConfig`/`NPCConfig` while preserving config schema/ENDEC behavior

### 📚 Documentation

### ⚙️ Miscellaneous Tasks

- Bump version
