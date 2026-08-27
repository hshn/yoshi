package yoshi

import zio.prelude.{Validation as _, *}

private[yoshi] trait NonEmptySetInstances { self: NonEmptyListInstances =>

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
    required: Validation[V, Option[NonEmptySet[A]], NonEmptySet[A]],
    element: Validation[V, A, B],
  ): Validation[V, Set[A], NonEmptySet[B]] = Validation.instance[Set[A]] { set =>
    for {
      as <- required.run(NonEmptySet.fromIterableOption(set))
      bs <- as.validateAs[NonEmptySet[B]]
    } yield {
      bs
    }
  }
}
