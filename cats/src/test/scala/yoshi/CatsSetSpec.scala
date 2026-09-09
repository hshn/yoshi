package yoshi

import yoshi.defaults.*
import yoshi.interop.cats.*
import zio.test.*

object CatsSetSpec extends ZIOSpecDefault {

  override def spec = suiteAll("collection → Set") {
    test("transforms every element") {
      for {
        fromList   <- List("1", "2", "3").validateAs[Set[Int]]
        fromVector <- Vector("1", "2", "3").validateAs[Set[Int]]
        fromSet    <- Set("1", "2", "3").validateAs[Set[Int]]
      } yield {
        assertTrue(
          fromList == Set(1, 2, 3),
          fromVector == Set(1, 2, 3),
          fromSet == Set(1, 2, 3),
        )
      }
    }
    test("drops the duplicates the input held") {
      for {
        result <- List("1", "1", "2").validateAs[Set[Int]]
      } yield {
        assertTrue(result == Set(1, 2))
      }
    }
    test("succeeds with the empty set on empty input") {
      for {
        result <- List.empty[String].validateAs[Set[Int]]
      } yield {
        assertTrue(result == Set.empty[Int])
      }
    }
    test("reports the violation under the index of the invalid element") {
      assertTrue(
        List("1", "abc").validateAs[Set[Int]].is(_.left) ==
          Violations.of(Violation.NonIntegerString("abc")).asChild(1),
      )
    }
    test("accumulates the violations of every invalid element") {
      assertTrue(
        List("abc", "2", "def").validateAs[Set[Int]].is(_.left) ==
          Violations.of(Violation.NonIntegerString("abc")).asChild(0) ++
          Violations.of(Violation.NonIntegerString("def")).asChild(2),
      )
    }
  }
}
