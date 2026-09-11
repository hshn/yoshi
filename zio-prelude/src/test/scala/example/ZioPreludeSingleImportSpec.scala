package example

import yoshi.*
import yoshi.defaults.*
import zio.prelude.Newtype
import zio.prelude.NonEmptyList
import zio.test.*

/** `import yoshi.*` alone, with no interop import: the module's instances and syntax reach the caller through the export in package
  * `yoshi`, the way the core syntax already does.
  */
object ZioPreludeSingleImportSpec extends ZIOSpecDefault {

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

  object PositiveInt extends Newtype[Int]
  type PositiveInt = PositiveInt.Type

  override def spec = suiteAll("import yoshi.* on its own") {
    test("resolves the collection instances") {
      for {
        result <- validation.run(Input(name = Some("42"), tags = List("1", "2")))
      } yield {
        assertTrue(result == Output(name = Some(42), tags = NonEmptyList(1, 2)))
      }
    }
    test("leaves an optional field free of an element index") {
      assertTrue(
        validation.run(Input(name = Some("abc"), tags = List("1"))).is(_.left) ==
          Violations.of(Violation.NonIntegerString("abc")).asChild("name"),
      )
    }
    test("resolves Validation.newtype") {
      val v = Validation.newtype(PositiveInt)((value, msg) => s"$value: $msg")
      for {
        result <- v.run(42)
      } yield {
        assertTrue(result == PositiveInt(42))
      }
    }
  }
}
