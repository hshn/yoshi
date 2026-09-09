package yoshi

import zio.test.*

object ValidationOutputTypeSpec extends ZIOSpecDefault {

  trait Name
  final class ShortName extends Name

  given Required[String]                    = Required("required")
  given Validation[String, Name, ShortName] = Validation.instance[Name](_ => Right(new ShortName))

  override def spec: Spec[TestEnvironment, Any] = suite("output type")(
    test("an instance producing a subtype is not selected when the supertype is asked for") {
      val name = new Name {}

      assertTrue(name.validateAs[Name] == Right(name))
    },
    test("the same holds through the Option derivation") {
      val name = new Name {}

      assertTrue(Some(name).validateAs[Name] == Right(name))
    },
  )
}
