package yoshi

/* Lifts the instances into package `yoshi`, so `import yoshi.*` is the whole import — having the jar on the classpath is enough.
 *
 * The instances stay in `yoshi.interop`, not here: this file cannot resolve any name without first elaborating its own export clause,
 * which needs those very signatures. Move a trait into package `yoshi` and the compiler reports the cycle as an error naming the trait
 * method, not this export clause — where it actually originates. See CONTRIBUTING.md for this module's other conventions.
 */
export yoshi.interop.cats.*
