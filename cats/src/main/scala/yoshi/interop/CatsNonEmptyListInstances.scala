package yoshi.interop

import _root_.cats.data.NonEmptyList
import _root_.cats.syntax.all.*
import yoshi.Required
import yoshi.Validation
import yoshi.Violations
import yoshi.syntax.all.*

private[interop] trait CatsNonEmptyListInstances extends CatsViolationsInstances {

  /** Validates each element of a `NonEmptyList`, reporting violations under the element index. */
  implicit def nonEmptyListCanBeValidatedAs[V, A, B](using
    validation: Validation[V, A, B],
  ): Validation[V, NonEmptyList[A], NonEmptyList[B]] =
    Validation.instance[NonEmptyList[A]] { as =>
      as.zipWithIndex.traverse { case (a, index) => validation.run(a).at(index).toValidated }.toEither
    }

  /** Validates each element of any collection into a `NonEmptyList`, failing with the [[Required]] violation when it is empty.
    *
    * The index in the violation path follows iteration order, which an unordered input such as `Set` does not fix.
    *
    * The input is bound to `Iterable` rather than derived from a `Traverse`, because `Option` has a `Traverse` instance too: a derivation
    * over `Traverse` would also match `Option[A]` to `Option[B]` and, winning from lexical scope, would push an element index into the path
    * of every optional field. `Option` is not an `Iterable`, so the bound rules that out by construction.
    */
  implicit def iterableCanBeValidatedAsNonEmptyList[V, A, B](using
    validation: Validation[V, A, B],
    required: Required[V],
  ): Validation[V, Iterable[A], NonEmptyList[B]] =
    Validation.instance[Iterable[A]] { as =>
      NonEmptyList.fromList(as.toList) match {
        case None      => Left(Violations.of(required.violation))
        case Some(nel) =>
          nel.zipWithIndex.traverse { case (a, index) => validation.run(a).at(index).toValidated }.toEither
      }
    }
}
