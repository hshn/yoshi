package example

import yoshi.*
import yoshi.defaults.*
import zio.Chunk
import zio.NonEmptyChunk
import zio.test.*

/** `import yoshi.*` alone, with no interop import: the module's instances reach the caller through the export in package `yoshi`, the way
  * the core syntax already does.
  *
  * This is what pins the instances as `implicit`, never `given`: a wildcard import skips `given` definitions, so converting them would take
  * them out of `import yoshi.*` and this file would stop compiling.
  */
object ZioSingleImportSpec extends ZIOSpecDefault {

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

  override def spec = suiteAll("import yoshi.* on its own") {
    test("resolves the chunk instances") {
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
