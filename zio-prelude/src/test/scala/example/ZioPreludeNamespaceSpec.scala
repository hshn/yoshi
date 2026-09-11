package example

import yoshi.*
import yoshi.defaults.*
import yoshi.interop.zioprelude.*
import zio.prelude.{Validation as _, *}
import zio.test.*

/** Written from outside the library, naming the provenance instead of relying on the export.
  *
  * The entry point sits at `yoshi.interop.zioprelude` rather than `yoshi.prelude` for the sake of these very imports: as a member of
  * package `yoshi`, the name `prelude` would be bound by `import yoshi.*` and would hide `zio.prelude` from every file that uses both.
  */
object ZioPreludeNamespaceSpec extends ZIOSpecDefault {

  case class Input(tags: List[String])
  case class Output(tags: NonEmptyList[Int])

  val validation: Validation[Violation, Input, Output] =
    Validation.cursor[Input] { c =>
      (
        c.validateAs[NonEmptyList[Int]](_.tags)
      ).validateN { tags =>
        Output(tags)
      }
    }

  override def spec = suiteAll("yoshi and zio-prelude imported side by side") {
    test("validates into the zio-prelude data type") {
      for {
        result <- validation.run(Input(tags = List("1", "2")))
      } yield {
        assertTrue(result == Output(tags = NonEmptyList(1, 2)))
      }
    }
    test("reports the empty input under the field path") {
      assertTrue(
        validation.run(Input(tags = Nil)).is(_.left) ==
          Violations.of(Violation.Required).asChild("tags"),
      )
    }
  }
}
