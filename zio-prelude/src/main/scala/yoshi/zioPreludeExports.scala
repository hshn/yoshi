package yoshi

/* Lifts the instances into package `yoshi`, so `import yoshi.*` is the whole import — having the jar on the classpath is enough.
 *
 * Three things hold this up, and each is easy to undo by accident:
 *
 * The instances are `implicit`, never `given`: a wildcard import skips `given` definitions, so moving them would take them out of
 * `import yoshi.*`.
 *
 * They live in `yoshi.interop`, not here: a file in package `yoshi` cannot resolve any name without first elaborating this export
 * clause, which would need those very signatures, and the compiler reports the resulting cycle far from its cause.
 *
 * The file name carries the module name because a top-level export compiles to `yoshi/<file name>$package.class`: two modules sharing a
 * file name collide on the classpath and one of the exports vanishes with no error.
 */
export yoshi.interop.zioprelude.*
