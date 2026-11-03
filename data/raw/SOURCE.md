# Source of `verbs.csv`

- **Origin:** OpenRussian.org dictionary data, published at
  https://github.com/Badestrand/russian-dictionary
- **Commit:** `50e210c4803237779cb562bc1abcea529066031c` (2021-08-09)
- **License:** CC BY-SA 4.0, see `LICENSE` in this folder
- **Changes:** line endings converted from CRLF to LF. Content is otherwise unchanged.

## Format notes

- Tab-separated, one header row, no quoting (`"` occurs inside German translations).
- Rows are ordered by word frequency (most frequent first). The importer uses the row
  number as frequency rank.
- Stress is marked with `'` after the stressed vowel. Monosyllables and forms with `ё`
  often carry no mark.
- Variant forms are separated by `,` or `//`.
- `presfut_*` holds the present tense for imperfective verbs and the simple future for
  perfective verbs.
- `partner` lists aspect partners separated by `;`, `-` means none.

All corrections go to `data/overrides.csv`. This file is never edited by hand.
