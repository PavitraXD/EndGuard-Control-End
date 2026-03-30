# End Guard

End Guard is a Paper plugin for Minecraft 1.21.x that gives admins full control over End access and the Ender Dragon fight.

## Modrinth Description

### EndGuard

Take complete control of the End on your Paper server.

End Guard lets admins:

- Lock or unlock access to the End instantly
- Block all teleports and portals into the End while locked
- Force players out of the End when it is locked
- Turn the Ender Dragon fight on or off independently
- Make the dragon peaceful and optionally freeze its movement
- Show boss bar status for Dragon Peace Mode or Dragon Fight Mode
- Schedule automatic unlocks or dragon enabling with a timer
- Persist all states across server restarts
- Support multiworld End dimensions

### Commands

```text
/end lock
/end unlock
/end dragon on
/end dragon off
/end status
/end timer <minutes>
```

### Permission

```text
end.admin
```

### Supported Platform

```text
Paper 1.21.x
Java 17+
```

## Features

- Independent End lock system and dragon control system
- End portal and teleport blocking
- Safe player evacuation from End worlds
- Dragon damage prevention, outgoing damage prevention, and heal blocking
- Configurable teleport destination
- Broadcasts, titles, sounds, and End boss bar feedback
- Persistent saved state

## Installation

1. Build or download the plugin jar.
2. Put `end-guard-1.0.0.jar` into your server `plugins` folder.
3. Start or restart the server.
4. Edit the generated config if you want custom teleport coordinates or dragon movement freeze.

## Configuration

Main options:

- `defaultEndLocked`
- `defaultDragonEnabled`
- `disableDragonMovement`
- `teleportLocation`
- `messages`

## Notes

- End access lock and dragon control are fully separate.
- If the End is unlocked, the dragon can still remain peaceful.
- If the End is locked, all player access attempts are blocked and players already inside are removed.
