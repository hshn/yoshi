package yoshi.interop

import _root_.zio.prelude.{Validation as _, *}
import yoshi.Validation
import yoshi.syntax.all.*

private[interop] trait NonEmptySetInstances extends NonEmptyListInstances {

  implicit def nonEmptySetValidation[V, A, B](using
    v: Validation[V, A, B],
  ): Validation[V, NonEmptySet[A], NonEmptySet[B]] = Validation.instance[NonEmptySet[A]] { as =>
    for {
      bs <- as.toNonEmptyList.validateAs[NonEmptyList[B]]
    } yield {
      NonEmptySet.fromNonEmptyList(bs)
    }
  }

  implicit def setCanBeNonEmptySet[V, A, B](using
    Validation[V, Option[NonEmptySet[A]], NonEmptySet[A]],
    Validation[V, A, B],
  ): Validation[V, Set[A], NonEmptySet[B]] = Validation.instance[Set[A]] { set =>
    for {
      as <- NonEmptySet.fromIterableOption(set).validateAs[NonEmptySet[A]]
      bs <- as.validateAs[NonEmptySet[B]]
    } yield {
      bs
    }
  }
}
