package yoshi.interop

import _root_.cats.Order
import _root_.cats.data.NonEmptySet
import yoshi.Required
import yoshi.Validation

private[interop] trait CatsNonEmptySetInstances extends CatsNonEmptyListInstances {

  /** Validates each element of a `NonEmptySet`, reporting violations under the index the element has in sorted order. */
  implicit def nonEmptySetCanBeValidatedAs[V, A, B](using
    Validation[V, A, B],
    Order[B],
  ): Validation[V, NonEmptySet[A], NonEmptySet[B]] =
    nonEmptyListCanBeValidatedAs[V, A, B]
      .contramap[NonEmptySet[A]](_.toNonEmptyList)
      .map(_.toNes)

  /** Validates each element of any collection into a `NonEmptySet`, failing with the [[Required]] violation when it is empty.
    *
    * The index in the violation path follows iteration order, which an unordered input such as `Set` does not fix.
    */
  implicit def iterableCanBeValidatedAsNonEmptySet[V, A, B](using
    Validation[V, A, B],
    Required[V],
    Order[B],
  ): Validation[V, Iterable[A], NonEmptySet[B]] =
    iterableCanBeValidatedAsNonEmptyList[V, A, B].map(_.toNes)
}
