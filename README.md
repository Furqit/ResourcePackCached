# **ResourcePackCached**

ResourcePackCached keeps server resource packs loaded, so they don't need to be reloaded when you rejoin, It also reapplies the last used server resource pack on game startup.

---

## **Primary Use Case**

Perfect for those who want to:

- Host resource packs on the server to simplify updates and reduce modpack size by removing local copies.
- Improve user experience with faster join times and no repeated loading.
- Speed up testing and debugging for mods or plugins that use resource packs.

---

## **Functionality**

**ResourcePackCached** runs automatically:

| Step | Description                           |
|------|---------------------------------------|
| 1    | Caches the server pack on first join. |
| 2    | Keeps it loaded after disconnecting.  |
| 3    | Skips reloading when rejoining.       |
| 4    | Reapplies it on game startup.         |

## **Support**

Need help or want to report an issue?
- Open an issue on the [GitHub repository](https://github.com/Furq07/resourcepackcached/issues)
- Join the [Discord server](https://discord.gg/XhZzmvzPDV)