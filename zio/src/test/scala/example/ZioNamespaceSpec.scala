package example

import yoshi.*
import yoshi.defaults.*
import yoshi.interop.zio.*
import zio.*
import zio.test.*

/** Written from outside the library, naming the provenance instead of relying on the export, next to a wildcard import of zio itself.
  *
  * The entry point sits at `yoshi.interop.zio` rather than `yoshi.zio`: as a member of package `yoshi`, the name `zio` would be bound by
  * `import yoshi.*` and a bare `zio.…` reference would become ambiguous for a caller who also writes `import zio.*`. This file writes both
  * wildcard imports and still names `Chunk` and `NonEmptyChunk` bare, so it stops compiling if the object ever moves into package `yoshi`.
  */
object ZioNamespaceSpec extends ZIOSpecDefault {

  case class Input(tags: Chunk[String])
  case class Output(tags: NonEmptyChunk[Int])

  val validation: Validation[Violation, Input, Output] =
    Validation.cursor[Input] { c =>
      (
        c.validateAs[NonEmptyChunk[Int]](_.tags)
      ).validateN { tags =>
        Output(tags)
      }
    }

  override def spec = suiteAll("yoshi and zio imported side by side") {
    test("validates into the zio data type") {
      for {
        result <- validation.run(Input(tags = Chunk("1", "2")))
      } yield {
        assertTrue(result == Output(tags = NonEmptyChunk(1, 2)))
      }
    }
    test("reports the empty input under the field path") {
      assertTrue(
        validation.run(Input(tags = Chunk.empty)).is(_.left) ==
          Violations.of(Violation.Required).asChild("tags"),
      )
    }
  }
}
