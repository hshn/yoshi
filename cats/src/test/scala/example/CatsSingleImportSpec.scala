package example

import cats.data.NonEmptyList
import cats.data.Validated
import cats.kernel.Monoid
import yoshi.*
import yoshi.defaults.*
import zio.test.*

/** `import yoshi.*` alone, with no interop import: the module's instances and syntax reach the caller through the export in package
  * `yoshi`, the way the core syntax already does.
  *
  * This is what pins the instances as `implicit`, never `given`: a wildcard import skips `given` definitions, so converting them would take
  * them out of `import yoshi.*` and this file would stop compiling.
  */
object CatsSingleImportSpec extends ZIOSpecDefault {

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

  override def spec = suiteAll("import yoshi.* on its own") {
    test("resolves the collection instances") {
      for {
        result <- validation.run(Input(name = Some("42"), tags = List("1", "2")))
      } yield {
        assertTrue(result == Output(name = Some(42), tags = NonEmptyList.of(1, 2)))
      }
    }
    test("resolves the Monoid") {
      assertTrue(Monoid[Violations[Violation]].empty == Violations.empty[Violation])
    }
    test("brings in the Validated syntax") {
      assertTrue(Validations.parseInt.runValidated("abc") == Validated.invalid(Violations.of(Violation.NonIntegerString("abc"))))
    }
    test("leaves an optional field free of an element index") {
      assertTrue(
        validation.run(Input(name = Some("abc"), tags = List("1"))).is(_.left) ==
          Violations.of(Violation.NonIntegerString("abc")).asChild("name"),
      )
    }
  }
}
