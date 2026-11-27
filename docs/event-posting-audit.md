# Core event posting audit

Thread entries describe the calling path. EventBus invokes subscribers inline.

| Poster | Event | Thread | Failure handling | Later work and partial state |
|---|---|---|---|---|
| ClientInitializer.initializeClient | ClientInitializationEvent | Fabric client initialization | Propagates | Dependencies are bound. A failure stops subsequent subscribers, configuration loading and the completion marker. Startup must report the failure. |
| Module.enable | ModuleToggleEvent | Caller | Captures notification failure | Other notification listeners stop. Enable skips onEnable on failure and rolls back. Subscription occurs before the guarded notification. |
| Module.disableInternal | ModuleToggleEvent | Caller | Captures failure, continues cleanup | Tasks are cancelled and the module is unsubscribed and marked disabled before notification. onToggle and onDisable still run. |
| MinecraftMixin.postDisconnectEvent | MinecraftDisconnectEvent | Minecraft disconnect caller | Propagates | The injection precedes connection close. A failure can skip later session cleanup subscribers and interrupt disconnect. A finally in one subscriber cannot guarantee delivery to other subscribers. |
| MinecraftMixin.postBeforeScreenChange | ScreenChangeEvent.Before | Screen transition caller | Propagates | Previous screen is captured. A failing subscriber interrupts the transition before cancellation is inspected. |
| MinecraftMixin.postAfterScreenInit | ScreenChangeEvent.AfterInit | Screen transition caller | Propagates | New screen initialization has run. A failure interrupts the remaining transition and later observers. |
| MinecraftMixin.postMinecraftTick | MinecraftTickEvent | Client tick | Propagates | A failure stops later tick work. |
| MinecraftMixin runTick hook | MinecraftRunTickEvent | Client loop | Propagates | Later observers are skipped. |

ConfigurationManager and TaskScheduler have no direct posting site in this review. EventFailureTracker retains a lifetime count and up to 64 recent summaries by default. A summary contains names and a truncated message. It holds no Throwable or stack trace.
