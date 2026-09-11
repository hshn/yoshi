package yoshi

/* Named after its module, not `package`: a top-level export compiles to `yoshi/<file name>$package.class`, so a file named `package`
 * here would collide with any other module that names its export file the same way, and one of the two would vanish from the
 * classpath with no error.
 */
export yoshi.syntax.all.*
