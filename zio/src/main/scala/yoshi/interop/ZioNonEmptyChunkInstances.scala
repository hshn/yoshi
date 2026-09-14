package yoshi.interop

import _root_.zio.Chunk
import _root_.zio.NonEmptyChunk
import yoshi.Validation
import yoshi.syntax.all.*

private[interop] trait ZioNonEmptyChunkInstances {

  /** Validates each element of a `NonEmptyChunk`, reporting violations under the element index. */
  implicit def nonEmptyChunkValidation[V, A, B](using
    v: Validation[V, A, B],
  ): Validation[V, NonEmptyChunk[A], NonEmptyChunk[B]] =
    Validation.instance[NonEmptyChunk[A]] { nec =>
      nec.zipWithIndex
        .map { case (a, index) => v.run(a).at(index) }
        .reduceMapLeft(_.map(NonEmptyChunk.single)) { (acc, result) =>
          (acc, result).validateN { case (bs, b) => bs :+ b }
        }
    }

  /** Validates a `Chunk` into a `NonEmptyChunk`, failing with the [[yoshi.Required]] violation when it is empty. */
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
