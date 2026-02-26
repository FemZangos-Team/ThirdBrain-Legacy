## Changelog

All notable changes to this project are documented in this release.

### 🐛 Bug Fixes


### 💼 Other


## [4.0.1] - 2026-02-25

### 🚀 Features

### 🐛 Bug Fixes

- Fix nearby block cache snapshots to prevent stale/unbounded growth and cross-thread mutation risks
- Fix conversation persistence to avoid new duplicate rows and restore chronological history order
- Register NPC death listener once during service initialization and delete config files on delete-by-type

### 💼 Other

- Improve NPC event queue/error handling and normalize config checkbox state updates

### 📚 Documentation

### ⚙️ Miscellaneous Tasks

- Bump version

