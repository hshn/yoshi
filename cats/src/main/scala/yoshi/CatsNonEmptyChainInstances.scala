package yoshi

import _root_.cats.data.NonEmptyChain
import _root_.cats.syntax.all.*

private[yoshi] trait CatsNonEmptyChainInstances extends CatsViolationsInstances {

  /** Validates each element of a `NonEmptyChain`, reporting violations under the element index. */
  implicit def nonEmptyChainCanBeValidatedAs[V, A, B](using
    validation: Validation[V, A, B],
  ): Validation[V, NonEmptyChain[A], NonEmptyChain[B]] =
    Validation.instance[NonEmptyChain[A]] { as =>
      as.zipWithIndex.traverse { case (a, index) => validation.run(a).at(index).toValidated }.toEither
    }

  /** Validates each element of any collection into a `NonEmptyChain`, failing with the [[Required]] violation when it is empty.
    *
    * The index in the violation path follows iteration order, which an unordered input such as `Set` does not fix.
    */
  implicit def iterableCanBeValidatedAsNonEmptyChain[V, A, B](using
    validation: Validation[V, A, B],
    required: Required[V],
  ): Validation[V, Iterable[A], NonEmptyChain[B]] =
    Validation.instance[Iterable[A]] { as =>
      NonEmptyChain.fromSeq(as.toSeq) match {
        case None      => Left(Violations.of(required.violation))
        case Some(nec) =>
          nec.zipWithIndex.traverse { case (a, index) => validation.run(a).at(index).toValidated }.toEither
      }
    }
}
