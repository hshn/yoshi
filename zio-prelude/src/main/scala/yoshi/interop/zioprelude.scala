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
  * It sits under `yoshi.interop` rather than at `yoshi.prelude`: as a member of package `yoshi`, the name `prelude` would be bound by
  * `import yoshi.*`, and a bare `prelude.…` reference becomes ambiguous for a caller who also writes `import zio.*`:
  *
  * {{{
  * Reference to prelude is ambiguous.
  * It is both imported by import yoshi._ and imported subsequently by import zio._
  * }}}
  */
object zioprelude
  extends AssociativeBothInstances
  with NonEmptyListInstances
  with NonEmptyChunkInstances
  with NonEmptySetInstances
  with NewtypeSyntax
