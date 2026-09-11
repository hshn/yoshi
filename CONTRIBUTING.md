# Contributing

This document covers what you need to know to add a new interop module to yoshi.

## Module layout

An interop module wraps a partner library's types with `Validation` instances, then lifts them into package `yoshi` so a caller only
ever writes `import yoshi.*`. Follow the layout the existing modules (`cats`, `zio-prelude`) already use:

- Instance traits live in `yoshi.interop`, one trait per concern (e.g. `CatsNonEmptyListInstances`).
- An aggregate object collects them: `yoshi.interop.<module>`, named after the module (`yoshi.interop.cats`, `yoshi.interop.zioprelude`).
  It composes its traits by `extends … with …`, never by self-type — extension makes each trait carry its own dependency, so the
  aggregate object doesn't have to enumerate them. A self-type only declares the dependency and pushes satisfying it onto whoever mixes
  the trait in; that would mean threading every trait added later through the aggregate object by hand. This is a maintenance
  preference, not something the compiler requires — self-types compile fine here too.
- A single `export yoshi.interop.<module>.*` line in package `yoshi`, in a file named after the module (`catsExports.scala`,
  `zioPreludeExports.scala`). Package `yoshi` cannot see the traits directly: resolving a name in that package would need to elaborate
  this export clause first, which needs those very signatures, so the traits have to stay out of package `yoshi` to avoid a cycle. If
  you move one in, the compiler reports the cycle at the trait, not at the export clause — expect a confusing error and know it means
  "move this back to `yoshi.interop`."

## Qualify partner-library references with `_root_.`

`yoshi.interop` holds an aggregate object named after each library it wraps — `yoshi.interop.cats` today, `yoshi.interop.zio` if
someone adds a module for zio core. That means an unqualified `cats.…` or `zio.…` written anywhere under `yoshi.interop` binds the
sibling aggregate object, not the library. Write `_root_.cats.…` / `_root_.zio.…` instead.

The zio-prelude module already writes `_root_.zio` even though nothing collides there yet — `yoshi.interop.zio` doesn't exist, so a
bare `zio.…` currently resolves to the library. It's qualified anyway because the qualification is cheap now and easy to forget to add
later, once some other module actually claims `yoshi.interop.zio`.

## One module per type

Only one module may provide the `Validation` instance for a given type, and the boundary is drawn at the library that owns the type —
a `Chunk` instance belongs in a module that depends on zio core, not on zio-prelude; a `Set` instance belongs in core, not in cats.
This isn't a style preference: instances export automatically into package `yoshi`, so if two modules ever provided an instance for the
same type, any caller with both jars on the classpath would hit an ambiguous-implicit compile error they can't work around without
dropping a jar. Issues #205 and #206 track the two places the codebase currently gets this wrong (`Chunk` in yoshi-zio-prelude, `Set` in
yoshi-cats) and need to move before they cause a real collision.

## Add your types to `AllInteropModulesSpec`

`interop-tests/src/test/scala/example/AllInteropModulesSpec.scala` is the only place in the build that puts every interop jar on one
classpath together. That makes it the only place a shared export file name or a duplicate instance for the same type can actually be
caught — everywhere else, each module compiles alone. It only catches what it exercises, though: a duplicate instance for a type this
spec doesn't touch would pass unnoticed. When you add a module, add its types to this spec too.
