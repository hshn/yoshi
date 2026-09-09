package example

import cats.data.NonEmptyList
import yoshi.*
import yoshi.defaults.*
import yoshi.interop.cats.*
import zio.test.*

/** Written from outside the library, with the imports a caller actually writes.
  *
  * The entry point sits at `yoshi.interop.cats` rather than `yoshi.cats` for the sake of these very imports: as a member of package
  * `yoshi`, the name `cats` would be bound by `import yoshi.*` and would hide the cats library from every file that uses both. Moving it
  * back stops this file from compiling.
  */
object CatsNamespaceSpec extends ZIOSpecDefault {

  case class Input(name: Option[String], tags: List[String])
  case class Output(name: Option[Int], tags: NonEmptyList[Int])

  val validation: Validation[Violation, Input, Output] =
    Validation.cursor[Input] { c =>
      (
        c.validateAs[Option[Int]](_.name),
        c.validateAs[NonEmptyList[Int]](_.tags),
      ).validateN { case (name, tags) =>
        Output(name, tags)
      }
    }

  override def spec = suiteAll("yoshi and cats imported side by side") {
    test("validates into the cats data type") {
      for {
        result <- validation.run(Input(name = Some("42"), tags = List("1", "2")))
      } yield {
        assertTrue(result == Output(name = Some(42), tags = NonEmptyList.of(1, 2)))
      }
    }
    test("reports each field under its own path") {
      assertTrue(
        validation.run(Input(name = Some("abc"), tags = Nil)).is(_.left) ==
          Violations.of(Violation.NonIntegerString("abc")).asChild("name") ++
          Violations.of(Violation.Required).asChild("tags"),
      )
    }
  }
}
