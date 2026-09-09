package yoshi

import yoshi.defaults.*
import yoshi.interop.cats.*
import zio.test.*

/** Guards the boundary between the cats collection instances and the `Option` derivations of the core module.
  *
  * `Option` has a cats `Traverse` instance, so an instance derived from `Traverse` would also cover `Option[A]` to `Option[B]`. Imported
  * into lexical scope it would outrank `optionCanBeValidatedAs`, and every optional field would report its violations one index deeper.
  * These tests fail the moment such a derivation reaches this module.
  */
object CatsOptionDerivationSpec extends ZIOSpecDefault {

  case class Input(name: Option[String], tags: List[String])
  case class Output(name: Option[Int], tags: List[Int])

  val validation: Validation[Violation, Input, Output] =
    Validation.cursor[Input] { c =>
      (
        c.validateAs[Option[Int]](_.name),
        c.validateAs[List[Int]](_.tags),
      ).validateN { case (name, tags) =>
        Output(name, tags)
      }
    }

  override def spec = suiteAll("Option derivations with the cats instances imported") {
    test("reports an optional value without an element index") {
      assertTrue(
        Some("abc").validateAs[Option[Int]].is(_.left) ==
          Violations.of(Violation.NonIntegerString("abc")),
      )
    }
    test("reports an optional field under the field path alone") {
      assertTrue(
        validation.run(Input(name = Some("abc"), tags = Nil)).is(_.left) ==
          Violations.of(Violation.NonIntegerString("abc")).asChild("name"),
      )
    }
    test("still indexes the elements of a field that is a collection") {
      assertTrue(
        validation.run(Input(name = None, tags = List("1", "abc"))).is(_.left) ==
          Violations.of(Violation.NonIntegerString("abc")).asChild(1).asChild("tags"),
      )
    }
    test("passes an absent optional field through") {
      for {
        result <- validation.run(Input(name = None, tags = Nil))
      } yield {
        assertTrue(result == Output(name = None, tags = Nil))
      }
    }
    test("validates a present optional field") {
      for {
        result <- validation.run(Input(name = Some("42"), tags = Nil))
      } yield {
        assertTrue(result == Output(name = Some(42), tags = Nil))
      }
    }
    test("still demands a value where the output type is not optional") {
      assertTrue(
        Option.empty[String].validateAs[Int].is(_.left) == Violations.of(Violation.Required),
      )
    }
  }
}
