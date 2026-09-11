package yoshi.interop

/** Everything yoshi offers for zio-prelude.
  *
  * Package `yoshi` exports these members, so having the module on the classpath is enough — `import yoshi.*` brings them in along with the
  * core syntax. Import this object directly to name the provenance instead.
  *
  * {{{
  * import yoshi.*
  * import yoshi.defaults.*
  * }}}
  *
  * It sits under `yoshi.interop` rather than at `yoshi.prelude`, because as a member of package `yoshi` the name would be bound by
  * `import yoshi.*` and would hide `zio.prelude` from every file that uses both.
  */
object zioprelude
  extends AssociativeBothInstances
  with NonEmptyListInstances
  with NonEmptyChunkInstances
  with NonEmptySetInstances
  with NewtypeSyntax
