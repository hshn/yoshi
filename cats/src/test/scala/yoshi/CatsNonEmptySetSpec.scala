package yoshi

import cats.data.NonEmptySet
import yoshi.defaults.*
import yoshi.interop.cats.*
import zio.test.*

object CatsNonEmptySetSpec extends ZIOSpecDefault {

  override def spec = suiteAll("NonEmptySet") {
    suiteAll("collection → NonEmptySet") {
      test("transforms every element") {
        for {
          fromList <- List("1", "2", "3").validateAs[NonEmptySet[Int]]
          fromSet  <- Set("1", "2", "3").validateAs[NonEmptySet[Int]]
        } yield {
          assertTrue(
            fromList == NonEmptySet.of(1, 2, 3),
            fromSet == NonEmptySet.of(1, 2, 3),
          )
        }
      }
      test("drops the duplicates the input held") {
        for {
          result <- List("1", "1", "2").validateAs[NonEmptySet[Int]]
        } yield {
          assertTrue(result == NonEmptySet.of(1, 2))
        }
      }
      test("fails with the Required violation when the collection is empty") {
        assertTrue(
          List.empty[String].validateAs[NonEmptySet[Int]].is(_.left) == Violations.of(Violation.Required),
        )
      }
      test("reports the violation under the index of the invalid element") {
        assertTrue(
          List("1", "abc").validateAs[NonEmptySet[Int]].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild(1),
        )
      }
    }
    suiteAll("NonEmptySet → NonEmptySet") {
      test("transforms every element") {
        for {
          result <- NonEmptySet.of("1", "2", "3").validateAs[NonEmptySet[Int]]
        } yield {
          assertTrue(result == NonEmptySet.of(1, 2, 3))
        }
      }
      test("reports the violation under the index the element has in sorted order") {
        assertTrue(
          NonEmptySet.of("1", "abc").validateAs[NonEmptySet[Int]].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild(1),
        )
      }
    }
  }
}
