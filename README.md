# **ResourcePackCached**

ResourcePackCached caches server resource packs so they never need to be re-downloaded when you rejoin. By default a cached pack only applies on the server that sent it (and is removed when you leave); an optional mode keeps the last pack loaded everywhere and reapplied on startup.

---

## **Primary Use Case**

Perfect for those who want to:

- Host resource packs on the server to simplify updates and reduce modpack size by removing local copies.
- Improve user experience with faster join times and no repeated loading.
- Speed up testing and debugging for mods or plugins that use resource packs.

---

## **Functionality**

**ResourcePackCached** runs automatically:

| Step | Description                                              |
|------|---------------------------------------------------------|
| 1    | Caches the server pack on first join.                   |
| 2    | Applies it only on that server; removes it when you leave. |
| 3    | Skips re-downloading when you rejoin.                    |

### Config (`config/rpc-config.json`)

| Key            | Default | Description                                                                                  |
|----------------|---------|----------------------------------------------------------------------------------------------|
| `keepGlobally` | `false` | `true` keeps the cached pack applied everywhere and reapplied at startup (the old behaviour). |

## **Support**

Need help or want to report an issue?
- Open an issue on the [GitHub repository](https://github.com/Furq07/resourcepackcached/issues)
- Join the [Discord server](https://discord.gg/XhZzmvzPDV)