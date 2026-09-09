package yoshi.interop

import yoshi.CatsNonEmptyChainInstances
import yoshi.CatsNonEmptyListInstances
import yoshi.CatsNonEmptySetInstances
import yoshi.CatsSetInstances
import yoshi.CatsValidatedSyntax
import yoshi.CatsViolationsInstances

/** Everything yoshi offers for cats.
  *
  * Package `yoshi` exports these members, so having the module on the classpath is enough — `import yoshi.*` brings them in along with the
  * core syntax. Import this object directly to name the provenance instead.
  *
  * {{{
  * import yoshi.*
  * import yoshi.defaults.*
  * }}}
  *
  * It sits under `yoshi.interop` rather than at `yoshi.cats`, because as a member of package `yoshi` the name would be bound by
  * `import yoshi.*` and would hide the cats library from every file that uses both.
  */
object cats
  extends CatsViolationsInstances
  with CatsNonEmptyListInstances
  with CatsNonEmptyChainInstances
  with CatsNonEmptySetInstances
  with CatsSetInstances
  with CatsValidatedSyntax
