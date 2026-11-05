# Glagolik (глаголик)

Android app for training Russian verb conjugation: present/future, past, imperative,
aspect and aspectual pairs, with spaced repetition per verb form.

- Offline only, no tracking, no Google Play Services
- Verb data: [OpenRussian](https://en.openrussian.org) (CC BY-SA 4.0), see [ATTRIBUTION.md](ATTRIBUTION.md)

Development guidelines: [CLAUDE.md](CLAUDE.md)

## Builds

- **Pull requests / main:** CI uploads a debug APK as workflow artifact (`glagolik-debug-…`).
- **Releases:** [release-please](https://github.com/googleapis/release-please) keeps a
  release PR open (version + `CHANGELOG.md` from Conventional Commits). Merging it creates
  the tag `vX.Y.Z` and the GitHub Release; the signed APK and its SHA-256 are attached.
  If the APK build failed (e.g. missing secrets), re-run *Actions → Release APK* with the tag.

### One-time repository setting

*Settings → Actions → General → Workflow permissions*: enable
"Allow GitHub Actions to create and approve pull requests" (needed for the release PR).

### Debug signing

CI signs debug APKs with a shared debug keystore from the secret
`GLAGOLIK_DEBUG_KEYSTORE_BASE64` (alias `androiddebugkey`, password `android`), so
new debug builds install as updates. For identical signatures in local builds, place the
same keystore at `~/.android/debug.keystore`. Without the secret, CI falls back to a
random key.

### One-time release signing setup

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
