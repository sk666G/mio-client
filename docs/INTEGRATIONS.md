# Integration and Notification Center

The integration status command reports local client state. It does not send network probes or disclose credentials.

## Status commands

| Command | Action |
| --- | --- |
| `;integrations` | Show Discord IPC, notification destination, Baritone and waypoint adapter state |
| `;webhook status` | Show destination, notification module state, queue, delivery state and failure count |
| `;webhook history` | Show the ten most recent delivery outcomes |
| `;webhook retry` | Enable the notification module if needed and resume a paused queue |
| `;webhook clearhistory` | Clear the delivery history without clearing queued messages |
| `;webhook get` | Describe the configured destination without displaying the URL path or credentials |

Destination management remains available through `;webhook set <url>`, `;webhook telegram <bot-token> <chat-id>`, `;webhook chatid <id>` and `;webhook clear`.

## Notification delivery behavior

- The pending queue is bounded to 1,000 messages and 1,000,000 characters.
- Batches contain at most 100 messages and are split to fit the destination's payload limit.
- Each HTTP request has a 30-second timeout. The webhook manager retries failed requests twice after the initial attempt, with short backoff delays.
- A batch stays in the queue until every chunk has been acknowledged. A partial failure can therefore cause an already-accepted chunk to be delivered again when the batch is retried; this is preferable to silently losing the remainder.
- After five consecutive failed batches, automatic delivery pauses. Use `;webhook retry` after correcting the destination or network problem.
- A successful batch removes its messages only after the complete batch still matches the queue head.

## Delivery history and privacy

The client keeps at most 20 recent delivery outcomes. Each record contains only a timestamp, outcome (delivered, failed or dropped), message count and a short technical detail such as an exception class or queue-full reason.

The history never stores chat message text, webhook payloads, webhook URL paths, bot tokens or chat contents. Clearing history does not clear the pending queue. The history is local and is not uploaded.

The Telegram bot token is runtime-only and is not written into the normal client configuration. Re-enter it after restarting Minecraft. The saved configuration keeps the Telegram destination type and chat ID, not the bot token.

## Integration state meanings

- **Discord IPC** reports through the shared integration-health vocabulary (disabled, connecting, connected, unavailable or failed) based on the local IPC connection future.
- **Webhook / Telegram** reports the configured destination using only its destination type and, for ordinary webhooks, its host. Telegram credentials are not shown.
- **Baritone** and **Xaero Waypoints** expose the same typed available/unavailable health state to commands and future GUI diagnostics. Xaero's unavailable state does not prove that Xaero itself is absent; it means Schizo is not currently connected to that adapter.

No integration is enabled merely by displaying its status. Network activity remains governed by its corresponding feature/module and explicit configuration.
