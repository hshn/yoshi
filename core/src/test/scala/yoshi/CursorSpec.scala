package yoshi

import yoshi.defaults.*
import zio.test.*

object CursorSpec extends ZIOSpecDefault {

  case class Input(
    name: Option[String],
    age: String,
    items: List[Input.Item],
    address: Input.Address,
  )

  object Input:
    case class Item(label: Option[String])
    case class Address(zip: Option[String])

  case class Output(
    name: String,
    age: Int,
    items: List[Output.Item],
  )

  object Output:
    case class Item(label: String)

  given Validation[Violation, Input.Item, Output.Item] =
    Validation.cursor[Input.Item] { c =>
      (
        c.field(_.label).validateAs[String]
      ).validateN { label =>
        Output.Item(label)
      }
    }

  val validation: Validation[Violation, Input, Output] =
    Validation.cursor[Input] { c =>
      (
        c.field(_.name).validateAs[String],
        c.field(_.age).validateAs[Int],
        c.field(_.items).validateAs[List[Output.Item]],
      ).validateN { case (name, age, items) =>
        Output(name, age, items)
      }
    }

  override def spec = suiteAll("Validation.cursor") {
    suiteAll("field") {
      test("transforms valid input") {
        val input = Input(
          name = Some("Alice"),
          age = "30",
          items = List(Input.Item(Some("item1")), Input.Item(Some("item2"))),
          address = Input.Address(Some("12345")),
        )
        val expected = Output(
          name = "Alice",
          age = 30,
          items = List(Output.Item("item1"), Output.Item("item2")),
        )

        for {
          result <- validation.run(input)
        } yield {
          assertTrue(result == expected)
        }
      }

      test("accumulates violations with correct paths") {
        val input = Input(
          name = None,
          age = "abc",
          items = List(Input.Item(Some("ok")), Input.Item(None)),
          address = Input.Address(None),
        )

        val expected = Violations[Violation](
          children = Map(
            Violations.Path("name")  -> Violations(Vector(Violation.Required)),
            Violations.Path("age")   -> Violations(Vector(Violation.NonIntegerString("abc"))),
            Violations.Path("items") -> Violations(
              children = Map(
                Violations.Path(1) -> Violations(Vector(Violation.Required)).asChild("label"),
              ),
            ),
          ),
        )

        assertTrue(validation.run(input).is(_.left) == expected)
      }

      test("produces same result as manual .at() calls") {
        val manualValidation: Validation[Violation, Input, Output] =
          Validation.instance[Input] { dirty =>
            (
              dirty.name.validateAs[String].at("name"),
              dirty.age.validateAs[Int].at("age"),
              dirty.items.validateAs[List[Output.Item]].at("items"),
            ).validateN { case (name, age, items) =>
              Output(name, age, items)
            }
          }

        val invalid = Input(name = None, age = "xyz", items = List(Input.Item(None)), address = Input.Address(None))

        val cursorResult = validation.run(invalid)
        val manualResult = manualValidation.run(invalid)
        assertTrue(cursorResult == manualResult)
      }
    }

    suiteAll("nested field access") {
      test("derives full path from nested accessor") {
        val v: Validation[Violation, Input, String] =
          Validation.cursor[Input] { c =>
            c.field(_.address.zip).validateAs[String]
          }

        val input = Input(name = None, age = "", items = Nil, address = Input.Address(None))

        val expected = Violations[Violation](
          children = Map(
            Violations.Path("address") -> Violations(
              children = Map(
                Violations.Path("zip") -> Violations(Vector(Violation.Required)),
              ),
            ),
          ),
        )
        assertTrue(v.run(input).is(_.left) == expected)
      }
    }

    suiteAll("field with .at() override") {
      test("uses overridden path instead of derived name") {
        val v: Validation[Violation, Input, String] =
          Validation.cursor[Input] { c =>
            c.field(_.name).at("display_name").validateAs[String]
          }

        val expected = Violations[Violation](
          children = Map(
            Violations.Path("display_name") -> Violations(Vector(Violation.Required)),
          ),
        )
        assertTrue(v.run(Input(name = None, age = "", items = Nil, address = Input.Address(None))).is(_.left) == expected)
      }
    }

    suiteAll("validateAs shorthand") {
      test("produces same result as field().validateAs") {
        val withField: Validation[Violation, Input, String] =
          Validation.cursor[Input] { c =>
            c.field(_.name).validateAs[String]
          }

        val withShorthand: Validation[Violation, Input, String] =
          Validation.cursor[Input] { c =>
            c.validateAs[String](_.name)
          }

        val invalid = Input(name = None, age = "", items = Nil, address = Input.Address(None))

        val fieldResult     = withField.run(invalid)
        val shorthandResult = withShorthand.run(invalid)
        assertTrue(fieldResult == shorthandResult)
      }
    }
    suiteAll("path") {
      test("derives the same path as field") {
        val derived: Validation[Nothing, Input, List[Violations.Paths]] =
          Validation.cursor[Input] { c =>
            Right(List(c.path(_.age), c.path(_.address.zip)))
          }

        val fromField: Validation[Nothing, Input, List[Violations.Paths]] =
          Validation.cursor[Input] { c =>
            Right(List(c.field(_.age).path, c.field(_.address.zip).path))
          }

        val input = Input(name = None, age = "", items = Nil, address = Input.Address(None))

        assertTrue(
          derived.run(input) == fromField.run(input),
          derived.run(input) == Right(
            List(
              Violations.Paths(List(Violations.Path.Key("age"))),
              Violations.Paths(List(Violations.Path.Key("address"), Violations.Path.Key("zip"))),
            ),
          ),
        )
      }

      test("places a rule's violation at a field while the fields keep their own") {
        given Validation[Violation, (Int, Int), CursorSpec.Period] =
          Validation
            .ensureOr[Violation, (Int, Int)] { case (since, until) => Violation.TooSmall(until, since) } { case (since, until) =>
              since <= until
            }
            .map(CursorSpec.Period.apply.tupled)

        val validation: Validation[Violation, CursorSpec.PeriodInput, CursorSpec.Period] =
          Validation.cursor[CursorSpec.PeriodInput] { c =>
            (
              c.validateAs[Int](_.since),
              c.validateAs[Int](_.until),
            ).andValidateAs[CursorSpec.Period](c.path(_.until))
          }

        assertTrue(
          validation.run(CursorSpec.PeriodInput(since = "x", until = "3")).is(_.left) ==
            Violations.of(Violation.NonIntegerString("x")).asChild("since"),
          validation.run(CursorSpec.PeriodInput(since = "10", until = "3")).is(_.left) ==
            Violations.of(Violation.TooSmall(3, 10)).asChild("until"),
        )
      }
    }
    suiteAll("field carried through unchanged") {
      test("combines with a field that is validated") {
        val validation: Validation[Violation, CursorSpec.Passthrough, CursorSpec.Parsed] =
          Validation.cursor[CursorSpec.Passthrough] { c =>
            (
              c.validateAs[String](_.id),
              c.validateAs[Int](_.age),
            ).validateN { case (id, age) =>
              CursorSpec.Parsed(id, age)
            }
          }

        for {
          result <- validation.run(CursorSpec.Passthrough(id = "abc", age = "30"))
        } yield {
          assertTrue(result == CursorSpec.Parsed(id = "abc", age = 30))
        }
      }
    }
  }

  case class Passthrough(id: String, age: String)
  case class Parsed(id: String, age: Int)
  case class PeriodInput(since: String, until: String)
  case class Period(since: Int, until: Int)
}
