package yoshi

import cats.syntax.all.*

private[yoshi] trait CatsSetInstances { self: CatsViolationsInstances =>

  /** Validates each element of any collection into a `Set`, reporting violations under the index the element had in the input. */
  implicit def iterableCanBeValidatedAsSet[V, A, B](using
    validation: Validation[V, A, B],
  ): Validation[V, Iterable[A], Set[B]] =
    Validation.instance[Iterable[A]] { as =>
      as.toList.zipWithIndex
        .traverse { case (a, index) => validation.run(a).at(index).toValidated }
        .toEither
        .map(_.toSet)
    }
}
