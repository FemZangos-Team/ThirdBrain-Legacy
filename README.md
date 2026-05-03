<h1 align="center" style="font-weight: normal;"><b>ThirdBrain</b></h1>
<p align="center"><img src="https://raw.githubusercontent.com/sailex428/SecondBrain/refs/heads/main/logo.png" alt="mod-logo"></p>

A Forge/Fabric mod that brings intelligent NPCs to your minecraft world. ThirdBrain is a fork of SecondBrain that focuses on in-world NPC behavior for prebuilt experiences, rather than companion-style automation. Create player-like characters controlled by LLMs that respond to chat and support authored gameplay.

**This mod works fully serverside. It must also be installed on a client for setup.**

## How ThirdBrain differs from original SecondBrain

ThirdBrain keeps the same core concept as SecondBrain, but shifts the design toward NPCs that belong to the world itself, not toward a player companion model. The goal is to improve how this system works inside adventure maps and story-driven maps, where NPCs need consistent lore-friendly behavior, predictable interactions, and tighter narrative control.

Recent changes include:
- Memory fragments
- Per-NPC conversation range
- Dedicated configuration screens for memory and zone behavior
- Refuse prohibited actions (attacking, mining, placing, breaking blocks)
- Zone-aware behavior injection with priority handling

### How memory fragments work
- A memory fragment is a persistent instruction or fact tied to one NPC that can be unlocked or locked.
- Use it for role rules, lore facts, relationships, or restrictions you want the NPC to unlock and keep over time.
- Creating a fragment with `/secondbrain memory create <unlocked|locked> <npcName> <memoryPrompt>` stores it and sets initial unlock state.
- Unlocking a fragment with `/secondbrain memory unlock <npcName> <memoryId>` enables an existing memory by id.
- Locking a fragment with `/secondbrain memory lock <npcName> <memoryId>` disables a previously unlocked memory by id.
- During chat/behavior, unlocked fragments are injected into that NPC's active context so responses stay consistent.
- Fragment lock state can also be edited directly in the Fragments screen via the `Unlocked` checkbox.
- Ideal use cases: 
   - An NPC that learns over time
   - NPC that unlocks new information as player progresses through a map

### How zone instructions work
- Zone instructions are location-based behavior rules for an NPC.
- You define instruction text for areas in the world, and the NPC applies those instructions while inside those zones.
- If more than one zone matches, priority handling decides which instruction set takes precedence.
- This allows the same NPC to behave differently in different places while keeping behavior deterministic.

Example:
- Within a Library, we might want the NPC to remember things like staying quiet or using text decorations to indicate whispering.
- Within an NPC's workplace, they might be focused on their job, while outside they could be more social and friendly.

## What I did not test

- I did not test the mod with the Player2 App, which soon will be tested.

## Requirements
      
- **Minecraft Version**: 1.16.5/1.18.2 (1.18.2 recommended)
- **Running Ollama server OR Player2 App OR an OpenAi Key**

## Mod Installation

1. **Download the Mod**:
   - Get the latest version of the `ThirdBrain` mod from the [Releases](https://github.com/mr2wei/ThirdBrain/releases) page.
2. **Install the Mod**:
   - Place `thirdbrain.jar` in your `mods` folder on your minecraft fabric server and client.
3. **Launch Minecraft**:
   - Start the Minecraft client and the server, and you're ready!

## Usage with Ollama/OpenAi

>Note: Player must be an operator to execute the following commands

### GUI
- Open the gui with the `/thirdbrain` command and create/spawn, despawn/delete or edit the NPCs there.
- Also edit the base configuration there.


### Commands

> Note: players need operator permission to run these server commands.

#### `/thirdbrain memory create <unlocked|locked> <npcName> <memoryPrompt>`
- Create a memory fragment for an NPC and set its initial state.
- Example unlocked: `/thirdbrain memory create unlocked Wanderer "Keep an eye out for incoming visitors and greet them warmly."`
- Example locked: `/thirdbrain memory create locked Wanderer "This memory is sensitive until unlocked later."`

#### `/thirdbrain memory unlock <npcName> <memoryId>`
- Unlock an existing memory fragment for an NPC so it can be used during chat/behavior.
- Example: `/thirdbrain memory unlock Wanderer home-mem-01`

#### `/thirdbrain memory lock <npcName> <memoryId>`
- Lock an existing unlocked memory fragment for an NPC so it is no longer injected during chat/behavior.
- Example: `/thirdbrain memory lock Wanderer home-mem-01`


**Interact with NPCs**:
- Just write in the chat to interact with the NPC.

## Setup Ollama
### Ollama (Local LLM)
1. **Download Ollama**:
   - Visit [Ollamas Website](https://ollama.com/) and download the installer for your operating system.

2. **Install and Run Ollama**:
   - Follow the setup instructions to install.
   - Start Ollama and ensure it's running in the background.

3. **Connect to the Mod**:
   - Use the gui to create/edit an NPC and put in the address to your ollama server (complete url with http://...; 
     if you're running your ollama on your local pc then you can just use the one that's already typed in)

## Setup OpenAi (Paid)
1. **Create API Key**
   - Visit [OpenAi's website](https://platform.openai.com/api-keys), sign up/log in and create a API key
   - Copy the created key
2. **Use the Key in the mod**
   - Open the GUI and create an NPC with the LLM Type `OPENAI` and paste key into the Key field. (The key wont be ever shared with any other clients that has access to the GUI, also not your client. It will be saved in a secondbrain config file on the server)

## Usage with Player2 App

>The mod will automatically sync the selected Characters from the player2 App to NPCs in your singleplayer world.
If you updated a description/name in the player2 App you can sync these changes with the command `/thirdbrain player2 SYNC`
It's not possible to change any configs from the Character with this mod.

**Interact with NPCs**
- Write messages in the chat or directly to a specific NPC
- Press and hold alt (win) or opt (macOS) key to talk directly to the LLM/AI
- Activate Text-To-Speech for any NPC in the config gui to hear the LLM/AI output

## Setup Player2 (Free, Limited)
1. **Download Player2 App**
   - Visit [player2's website](https://player2.game) and download the App for your operating system.

2. **Start the App and select Characters**
   - Start the downloaded App and select the Characters you wanna use as NPCs

## License

This project is licensed under the [LGPL-3.0](https://github.com/mr2wei/ThirdBrain/blob/main/LICENSE.md)
## [Disclaimer](https://github.com/mr2wei/ThirdBrain/blob/main/DISCLAIMER.md)

## Credits
This project utilizes components from the following projects:
- [fabric-carpet](https://github.com/gnembon/fabric-carpet)
- [automatone](https://github.com/Ladysnake/Automatone)
- [player2](https://player2.game)
- [SecondBrain](https://github.com/sailex428/secondbrain)

Thank you to the developers of these projects for their amazing work!

## Author admissions
- I highkey vibe coded all my changes sorry
- shoutout codex 5.3
