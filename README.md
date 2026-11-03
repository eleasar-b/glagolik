# Glagolik (глаголик)

Android app for training Russian verb conjugation: present/future, past, imperative,
aspect and aspectual pairs, with spaced repetition per verb form.

- Offline only, no tracking, no Google Play Services
- Verb data: [OpenRussian](https://en.openrussian.org) (CC BY-SA 4.0), see [ATTRIBUTION.md](ATTRIBUTION.md)

Development guidelines: [CLAUDE.md](CLAUDE.md)

## Builds

- **Pull requests / main:** CI uploads a debug APK as workflow artifact (`glagolik-debug-…`).
- **Releases:** pushing a tag `vX.Y.Z` builds a signed release APK and publishes it as
  GitHub Release, together with a SHA-256 checksum.

### One-time signing setup

```sh
keytool -genkeypair -v -keystore glagolik-release.keystore -alias glagolik \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 glagolik-release.keystore   # macOS: base64 -i glagolik-release.keystore
```

Add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `GLAGOLIK_KEYSTORE_BASE64` | base64 output from above |
| `GLAGOLIK_KEYSTORE_PASSWORD` | keystore password |
| `GLAGOLIK_KEY_ALIAS` | `glagolik` |
| `GLAGOLIK_KEY_PASSWORD` | key password (same as keystore password for PKCS12 keystores) |

Keep the keystore file in a password manager. Losing it means updates can no longer be
installed over existing installs.

## License

Code: [Apache License 2.0](LICENSE). Data in `data/`: CC BY-SA 4.0.
