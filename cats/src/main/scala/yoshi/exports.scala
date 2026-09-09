package yoshi

/* Lifts the instances into package `yoshi` so that `import yoshi.*` is the whole import, the way the core syntax already reaches callers.
 *
 * Computing the members of package `yoshi` now runs through this export, so an unqualified `cats.…` anywhere in this module would ask
 * whether `cats` is one of those members while they are still being computed. That is why the module writes `_root_.cats`, and why its
 * instance traits compose by extension rather than by self-type. */
export yoshi.interop.cats.*
