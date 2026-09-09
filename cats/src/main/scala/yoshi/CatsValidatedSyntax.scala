package yoshi

import cats.data.Validated

private[yoshi] trait CatsValidatedSyntax {

  extension [V, A, B](self: Validation[V, A, B])

    /** Run this validation into `Validated`, so its result composes with the accumulating cats combinators.
      *
      * {{{
      * (nameValidation.runValidated(input.name), ageValidation.runValidated(input.age))
      *   .mapN(Member.apply)
      * }}}
      */
    def runValidated(a: A): Validated[Violations[V], B] = Validated.fromEither(self.run(a))

  extension (self: Validation.type)

    /** Build a [[Validation]] from a function that reports failures as `Validated`. */
    def fromValidated[V, A, B](f: A => Validated[Violations[V], B]): Validation[V, A, B] =
      Validation.instance[A](a => f(a).toEither)
}
