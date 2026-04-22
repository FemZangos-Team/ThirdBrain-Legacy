## Changelog

All notable changes to this project are documented in this release.

## [4.0.2] - 2026-04-22

### 🐛 Bug Fixes

- Fix zone behavior screen save button not persisting config (now sends UpdateNpcConfigPacket on save)
- Reset all NPCs to inactive on service startup to prevent stale active states across restarts
- Call NPCSpawner.remove() when removing an NPC that was never spawned to prevent orphaned entities

### 🚀 Features

- LLM client now initialized before NPC spawn; unreachable LLM logs an error in chat and marks NPC inactive instead of crashing
- Wrap post-spawn NPC creation in error handling; removes spawned entity and marks NPC inactive on failure
- Improve OpenWebUI error messages to include root cause, endpoint URL, truncated prompt, and HTTP response body
- Force HTTP/1.1 in OpenWebUI client to avoid protocol negotiation failures
- Increase default LLM timeout from 10s to 60s

### ⚙️ Miscellaneous Tasks

- Simplify LLM system prompt by removing redundant behavior rules, body-language action references, and idle command defaults
- Expose `initLLMClient` as `internal` in NPCFactory for use by NPCService
- Add `docs/`, `.metals/`, and `.settings/` to .gitignore

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
