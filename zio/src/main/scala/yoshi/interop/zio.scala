package yoshi.interop

/** Everything yoshi offers for zio core.
  *
  * Package `yoshi` exports these members, so having the module on the classpath is enough — `import yoshi.*` brings them in along with the
  * core syntax. Import this object directly to name the provenance instead.
  *
  * {{{
  * import yoshi.*
  * import yoshi.defaults.*
  * }}}
  *
  * It sits under `yoshi.interop` rather than at `yoshi.zio`, because as a member of package `yoshi` the name would be bound by
  * `import yoshi.*` and would hide the zio library from every file that uses both.
  */
object zio extends ZioNonEmptyChunkInstances
