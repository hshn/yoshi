package yoshi

import _root_.cats.syntax.all.*

private[yoshi] trait CatsSetInstances extends CatsViolationsInstances {

  /** Validates each element of any collection into a `Set`, reporting violations under the index the element had in the input.
    *
    * The index follows iteration order, which an unordered input such as `Set` or `HashSet` does not fix: the same elements can land on
    * different indices from one run to the next. Where the path has to be stable, validate from an ordered collection.
    */
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
