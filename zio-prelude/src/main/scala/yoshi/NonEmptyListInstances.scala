package yoshi

import zio.prelude.{Validation as _, *}

private[yoshi] trait NonEmptyListInstances { self: AssociativeBothInstances =>

  implicit def nonEmptyListValidation[V, A, B](using
    v: Validation[V, A, B],
  ): Validation[V, NonEmptyList[A], NonEmptyList[B]] =
    Validation.instance[NonEmptyList[A]] { nel =>
      nel.zipWithIndex.forEach1 { case (a, index) =>
        v.run(a).at(index)
      }
    }

  implicit def listCanBeNonEmptyList[V, A, B](using
    required: Validation[V, Option[NonEmptyList[A]], NonEmptyList[A]],
    element: Validation[V, A, B],
  ): Validation[V, List[A], NonEmptyList[B]] = Validation.instance[List[A]] { list =>
    for {
      as <- required.run(NonEmptyList.fromIterableOption(list))
      bs <- as.validateAs[NonEmptyList[B]]
    } yield {
      bs
    }
  }
}
