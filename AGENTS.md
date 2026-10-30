Before task work, read `CONTEXT.md` (technical map and working rules). Read
`README.md` only for status/setup and `HISTORY.md` only when history matters.

`ingest/` is local reference material for other open-source projects: never use it
as a build input and never copy their source. Do not recreate `tasks/` or `archive/`
or maintain draw.io files.

Update `README.md`, `CONTEXT.md`, `HISTORY.md` and this file only in the same commit
as the code change. `HISTORY.md` is append-only; never rewrite it.

Subagents are for read-only exploration or independent files/worktrees. Never
parallelize edits to `core.kt` or `layout.kt`; keep `MainActivity.kt` as a thin entry
point and put screen code in its section file.
