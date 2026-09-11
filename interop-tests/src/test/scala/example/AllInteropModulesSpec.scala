package example

import yoshi.*
import yoshi.defaults.*
import zio.test.*

/** Both interop modules on one classpath, reached through `import yoshi.*` alone.
  *
  * Each module lifts its instances into package `yoshi` from its own jar. Nothing else in the build puts the two jars together, so this is
  * the only place where a name they both claim — a shared export file name, an instance for the same type — can be caught.
  */
object AllInteropModulesSpec extends ZIOSpecDefault {

  case class Input(catsTags: List[String], zioTags: List[String])
  case class Output(
    catsTags: _root_.cats.data.NonEmptyList[Int],
    zioTags: _root_.zio.prelude.NonEmptyList[Int],
  )

  val validation: Validation[Violation, Input, Output] =
    Validation.cursor[Input] { c =>
      (
        c.validateAs[_root_.cats.data.NonEmptyList[Int]](_.catsTags),
        c.validateAs[_root_.zio.prelude.NonEmptyList[Int]](_.zioTags),
      ).validateN { case (catsTags, zioTags) =>
        Output(catsTags, zioTags)
      }
    }

  override def spec = suiteAll("cats and zio-prelude on one classpath") {
    test("resolves both modules' instances from import yoshi.* alone") {
      for {
        result <- validation.run(Input(catsTags = List("1"), zioTags = List("2")))
      } yield {
        assertTrue(
          result == Output(
            catsTags = _root_.cats.data.NonEmptyList.of(1),
            zioTags = _root_.zio.prelude.NonEmptyList(2),
          ),
        )
      }
    }
    test("keeps each module's violations under its own field") {
      assertTrue(
        validation.run(Input(catsTags = Nil, zioTags = Nil)).is(_.left) ==
          Violations.of(Violation.Required).asChild("catsTags") ++
          Violations.of(Violation.Required).asChild("zioTags"),
      )
    }
  }
}
