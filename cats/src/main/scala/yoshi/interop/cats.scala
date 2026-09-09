package yoshi.interop

import yoshi.CatsNonEmptyChainInstances
import yoshi.CatsNonEmptyListInstances
import yoshi.CatsNonEmptySetInstances
import yoshi.CatsSetInstances
import yoshi.CatsValidatedSyntax
import yoshi.CatsViolationsInstances

/** Everything yoshi offers for cats, in one import.
  *
  * {{{
  * import yoshi.*
  * import yoshi.defaults.*
  * import yoshi.interop.cats.*
  * }}}
  *
  * It sits under `yoshi.interop` rather than at `yoshi.cats`, because `import yoshi.*` would then bind the name `cats` and hide the cats
  * library itself from every file that uses both.
  */
object cats
  extends CatsViolationsInstances
  with CatsNonEmptyListInstances
  with CatsNonEmptyChainInstances
  with CatsNonEmptySetInstances
  with CatsSetInstances
  with CatsValidatedSyntax
