package yoshi

import yoshi.Violations.Path
import yoshi.Violations.Paths
import zio.test.Gen
import zio.test.ZIOSpecDefault
import zio.test.assertTrue
import zio.test.check

object ViolationsSpec extends ZIOSpecDefault {
  // Keys are drawn from a small space so that duplicate keys occur regularly.
  private val genKey: Gen[Any, Path | String | Int] =
    Gen.oneOf(
      Gen.elements("a", "b", "c"),
      Gen.int(0, 2),
      Gen.elements(Path.Key("a"), Path.Index(0)),
    )

  private def genViolations(depth: Int): Gen[Any, Violations[Int]] =
    for
      values   <- Gen.vectorOfBounded(0, 2)(Gen.int(0, 9))
      children <-
        if depth == 0 then Gen.const(Map.empty[Path, Violations[Int]])
        else Gen.mapOfBounded(0, 2)(genKey.map(pathOf), genViolations(depth - 1))
    yield Violations(values, children)

  private val genEntries: Gen[Any, List[(Path | String | Int, Violations[Int])]] =
    Gen.listOfBounded(0, 6)(genKey <*> genViolations(2))

  private def pathOf(key: Path | String | Int): Path = key match
    case path: Path  => path
    case key: String => Path.Key(key)
    case index: Int  => Path.Index(index)

  // The oracle uses the existing asChild overloads, so it never touches the code under test.
  private def viaAsChild(entries: List[(Path | String | Int, Violations[Int])]): Violations[Int] =
    entries.foldLeft(Violations.empty[Int]) {
      case (acc, (path: Path, child))  => acc ++ child.asChild(path)
      case (acc, (key: String, child)) => acc ++ child.asChild(key)
      case (acc, (index: Int, child))  => acc ++ child.asChild(index)
    }

  override def spec = suiteAll("Violations") {
    test("++ merges two Violations recursively") {
      val a = Violations(
        values = Vector("a", "b"),
        children = Map(
          Path.Key("c") -> Violations(
            values = Vector("d"),
          ),
          Path.Key("e") -> Violations(
            values = Vector("f"),
          ),
          Path.Index(1) -> Violations(
            values = Vector("g"),
          ),
          Path.Index(2) -> Violations(
            values = Vector("h"),
          ),
        ),
      )
      val b = Violations(
        values = Vector("b", "c"),
        children = Map(
          Path.Key("e") -> Violations(
            values = Vector("f"),
          ),
          Path.Key("g") -> Violations(
            values = Vector("h"),
          ),
          Path.Index(2) -> Violations(
            values = Vector("i"),
          ),
          Path.Index(3) -> Violations(
            values = Vector("j"),
          ),
        ),
      )

      val c = Violations(
        values = Vector("a", "b", "b", "c"),
        children = Map(
          Path.Key("c") -> Violations(
            values = Vector("d"),
          ),
          Path.Key("e") -> Violations(
            values = Vector("f", "f"),
          ),
          Path.Key("g") -> Violations(
            values = Vector("h"),
          ),
          Path.Index(1) -> Violations(
            values = Vector("g"),
          ),
          Path.Index(2) -> Violations(
            values = Vector("h", "i"),
          ),
          Path.Index(3) -> Violations(
            values = Vector("j"),
          ),
        ),
      )

      assertTrue(a ++ b == c)
    }
    test("map transforms values") {
      val v = Violations(values = Vector(1, 2, 3))
      assertTrue(v.map(_ * 10) == Violations(values = Vector(10, 20, 30)))
    }
    test("map transforms children recursively") {
      val v = Violations(
        values = Vector(1),
        children = Map(
          Path.Key("a") -> Violations(
            values = Vector(2),
            children = Map(Path.Index(0) -> Violations(values = Vector(3))),
          ),
        ),
      )
      val expected = Violations(
        values = Vector("1"),
        children = Map(
          Path.Key("a") -> Violations(
            values = Vector("2"),
            children = Map(Path.Index(0) -> Violations(values = Vector("3"))),
          ),
        ),
      )
      assertTrue(v.map(_.toString) == expected)
    }
    test("map on empty returns empty") {
      val v = Violations.empty[Int]
      assertTrue(v.map(_.toString) == Violations.empty[String])
    }
    suiteAll("children") {
      test("nests children under String and Int keys") {
        val a = Violations.of("a")
        val b = Violations.of("b")
        assertTrue(
          Violations.children("name" -> a, 0 -> b) == a.asChild("name") ++ b.asChild(0),
        )
      }
      test("nests a child under a Path key") {
        val a = Violations.of("a")
        assertTrue(Violations.children(Path.Key("name") -> a) == a.asChild(Path.Key("name")))
      }
      test("merges entries that share a key") {
        val x = Violations.of("x")
        val y = Violations.of("y")
        assertTrue(
          Violations.children("k" -> x, "k" -> y) == x.asChild("k") ++ y.asChild("k"),
        )
      }
      test("no entries yields empty") {
        assertTrue(Violations.children[String]() == Violations.empty[String])
      }
      test("equals the asChild ++ formulation for any keys and subtrees") {
        check(genEntries) { entries =>
          assertTrue(Violations.children(entries*) == viaAsChild(entries))
        }
      }
      test("each child is the merge of every entry at its key, and nothing else") {
        check(genEntries) { entries =>
          val result = Violations.children(entries*)
          val byPath = entries.groupMap { case (key, _) => pathOf(key) } { case (_, child) => child }
          assertTrue(
            result.values.isEmpty,
            result.children.keySet == byPath.keySet,
            byPath.forall { case (path, children) => result.children.get(path) == Some(children.reduce(_ ++ _)) },
          )
        }
      }
    }
    suiteAll("toList") {
      test("empty Violations returns empty list") {
        assertTrue(Violations.empty[String].toList == Nil)
      }
      test("flat Violations returns values with empty paths") {
        val violations = Violations(values = Vector("a", "b"))
        assertTrue(
          violations.toList == List(
            (Paths.empty, "a"),
            (Paths.empty, "b"),
          ),
        )
      }
      test("nested Violations returns values with paths") {
        val violations = Violations[String](
          children = Map(
            Path.Key("name") -> Violations(values = Vector("required")),
          ),
        )
        assertTrue(
          violations.toList == List(
            (Paths(List(Path.Key("name"))), "required"),
          ),
        )
      }
      test("deeply nested Violations returns full paths") {
        val violations = Violations[String](
          children = Map(
            Path.Key("address") -> Violations[String](
              children = Map(
                Path.Key("zip") -> Violations(values = Vector("invalid")),
              ),
            ),
          ),
        )
        assertTrue(
          violations.toList == List(
            (Paths(List(Path.Key("address"), Path.Key("zip"))), "invalid"),
          ),
        )
      }
      test("mixed root and nested values are all included") {
        val violations = Violations(
          values = Vector("root-error"),
          children = Map(
            Path.Key("field") -> Violations(values = Vector("field-error")),
            Path.Index(0)     -> Violations(values = Vector("index-error")),
          ),
        )
        val result = violations.toList
        assertTrue(
          result.contains((Paths.empty, "root-error")),
          result.contains((Paths(List(Path.Key("field"))), "field-error")),
          result.contains((Paths(List(Path.Index(0))), "index-error")),
          result.length == 3,
        )
      }
      test("children are flattened in a deterministic path order") {
        val violations = Violations[String](
          children = Map(
            Path.Index(10)    -> Violations(values = Vector("index-10")),
            Path.Key("zeta")  -> Violations(values = Vector("zeta")),
            Path.Index(2)     -> Violations(values = Vector("index-2")),
            Path.Key("alpha") -> Violations(
              children = Map(
                Path.Key("beta") -> Violations(values = Vector("alpha.beta")),
              ),
            ),
          ),
        )

        assertTrue(
          violations.toList == List(
            (Paths(List(Path.Key("alpha"), Path.Key("beta"))), "alpha.beta"),
            (Paths(List(Path.Key("zeta"))), "zeta"),
            (Paths(List(Path.Index(2))), "index-2"),
            (Paths(List(Path.Index(10))), "index-10"),
          ),
        )
      }
    }
    suiteAll("Paths#toString") {
      test("empty paths") {
        assertTrue(Paths.empty.toString == "")
      }
      test("single key") {
        assertTrue(Paths(List(Path.Key("name"))).toString == "name")
      }
      test("nested keys") {
        assertTrue(Paths(List(Path.Key("address"), Path.Key("zip"))).toString == "address.zip")
      }
      test("single index") {
        assertTrue(Paths(List(Path.Index(0))).toString == "[0]")
      }
      test("key followed by index") {
        assertTrue(Paths(List(Path.Key("items"), Path.Index(0))).toString == "items[0]")
      }
      test("key, index, key") {
        assertTrue(Paths(List(Path.Key("items"), Path.Index(0), Path.Key("name"))).toString == "items[0].name")
      }
      test("index followed by key") {
        assertTrue(Paths(List(Path.Index(0), Path.Key("name"))).toString == "[0].name")
      }
    }
  }
}
