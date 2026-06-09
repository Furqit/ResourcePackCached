### Updates & Improvements
- **Per-server packs (fixes "cached pack applies everywhere").** Cached server packs now apply only on the
  server that sent them and are removed when you leave — the download is still cached, so rejoining never
  re-downloads. Previously a cached pack was force-applied at startup and kept applied after you
  disconnected, so it bled into singleplayer / the main menu / other servers.
- **Toggle in `config/rpc-config.json`.** Set `"keepGlobally": true` to restore the old behaviour (keep
  cached packs applied everywhere / at startup). Defaults to `false`.

### Previous
- Updated to support 26.1.x.
