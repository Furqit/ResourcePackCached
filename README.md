# **ResourcePackCached**

**ResourcePackCached** improves how Minecraft handles server resource packs by preventing them from being unloaded when disconnecting. This allows the pack to stay loaded across sessions, so players don’t have to reload it when rejoining the same server. It also reapplies the last used server resource pack on game startup—saving time and improving the overall experience.

---

## **Primary Use Case**

Perfect for those who want to:

- Host resource packs on the server to simplify updates and reduce modpack size by removing local copies.
- Improve user experience with faster join times and no repeated loading.
- Speed up testing and debugging for mods or plugins that use resource packs.

---

## **Functionality**

**ResourcePackCached** works automatically in the background:

| Step | Description |
|------|-------------|
| 1 | Downloads and caches the server resource pack on the first join. |
| 2 | Keeps the pack loaded after disconnecting. |
| 3 | Skips reloading the pack when rejoining the same server. |
| 4 | Reapplies the cached resource pack automatically on game startup. |

---

## **Support**

Need help or want to report an issue?
- Open an issue on the [GitHub repository](https://github.com/Furq07/resourcepackcached/issues)
- Join the [Discord server](https://discord.gg/XhZzmvzPDV)

---

## **Partner**

<p align="center">  
  <a href="https://billing.revivenode.com/aff.php?aff=517">  
    <img src="https://versions.revivenode.com/resources/banner_wide_one.gif" alt="Revivenode Banner">  
  </a>  
</p>  
<p align="center"> Use code <strong>FURQ</strong> for 15% off your order! </p>