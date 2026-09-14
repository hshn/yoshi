package example

import yoshi.*
import yoshi.defaults.*
import zio.test.*

/** Every interop module on one classpath, reached through `import yoshi.*` alone.
  *
  * Each module lifts its instances into package `yoshi` from its own jar. A top-level export compiles to `yoshi/<file name>$package.class`,
  * so two modules sharing a file name collide on the classpath and one export vanishes with no error and no warning. Nothing else in the
  * build puts the jars together, so this file's compilation is the only thing that catches it — either that collision, or an instance two
  * modules both claim for the same type.
  *
  * The two halves don't catch equally well: losing any export breaks this file's compilation outright, unconditionally. A duplicate
  * instance is only caught for the types this spec exercises below — a future duplicate on some other type would pass unnoticed here.
  * Whoever adds a new interop module should add its types to this spec too.
  */
object AllInteropModulesSpec extends ZIOSpecDefault {

  case class Input(catsTags: List[String], zioPreludeTags: List[String], zioTags: _root_.zio.Chunk[String])
  case class Output(
    catsTags: _root_.cats.data.NonEmptyList[Int],
    zioPreludeTags: _root_.zio.prelude.NonEmptyList[Int],
    zioTags: _root_.zio.NonEmptyChunk[Int],
  )

  val validation: Validation[Violation, Input, Output] =
    Validation.cursor[Input] { c =>
      (
        c.validateAs[_root_.cats.data.NonEmptyList[Int]](_.catsTags),
        c.validateAs[_root_.zio.prelude.NonEmptyList[Int]](_.zioPreludeTags),
        c.validateAs[_root_.zio.NonEmptyChunk[Int]](_.zioTags),
      ).validateN { case (catsTags, zioPreludeTags, zioTags) =>
        Output(catsTags, zioPreludeTags, zioTags)
      }
    }

  override def spec = suiteAll("cats, zio-prelude and zio on one classpath") {
    test("resolves every module's instances from import yoshi.* alone") {
      for {
        result <- validation.run(
          Input(catsTags = List("1"), zioPreludeTags = List("2"), zioTags = _root_.zio.Chunk("3")),
        )
      } yield {
        assertTrue(
          result == Output(
            catsTags = _root_.cats.data.NonEmptyList.of(1),
            zioPreludeTags = _root_.zio.prelude.NonEmptyList(2),
            zioTags = _root_.zio.NonEmptyChunk(3),
          ),
        )
      }
    }
    test("keeps each module's violations under its own field") {
      assertTrue(
        validation.run(Input(catsTags = Nil, zioPreludeTags = Nil, zioTags = _root_.zio.Chunk.empty)).is(_.left) ==
          Violations.of(Violation.Required).asChild("catsTags") ++
          Violations.of(Violation.Required).asChild("zioPreludeTags") ++
          Violations.of(Violation.Required).asChild("zioTags"),
      )
    }
  }
}
