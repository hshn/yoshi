package yoshi

import zio.Chunk
import zio.NonEmptyChunk
import zio.prelude.{Validation as _, *}

private[yoshi] trait NonEmptyChunkInstances { self: AssociativeBothInstances =>

  implicit def nonEmptyChunkValidation[V, A, B](using
    v: Validation[V, A, B],
  ): Validation[V, NonEmptyChunk[A], NonEmptyChunk[B]] =
    Validation.instance[NonEmptyChunk[A]] { nec =>
      nec.zipWithIndex.forEach1 { case (a, index) =>
        v.run(a).at(index)
      }
    }

  implicit def chunkCanBeNonEmptyChunk[V, A, B](using
    Validation[V, Option[NonEmptyChunk[A]], NonEmptyChunk[A]],
    Validation[V, A, B],
  ): Validation[V, Chunk[A], NonEmptyChunk[B]] = Validation.instance[Chunk[A]] { chunk =>
    for {
      as <- NonEmptyChunk.fromIterableOption(chunk).validateAs[NonEmptyChunk[A]]
      bs <- as.validateAs[NonEmptyChunk[B]]
    } yield {
      bs
    }
  }
}
