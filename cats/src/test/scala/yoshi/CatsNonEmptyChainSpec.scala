package yoshi

import cats.data.NonEmptyChain
import yoshi.defaults.*
import yoshi.interop.cats.*
import zio.test.*

object CatsNonEmptyChainSpec extends ZIOSpecDefault {

  override def spec = suiteAll("NonEmptyChain") {
    suiteAll("collection → NonEmptyChain") {
      test("transforms every element") {
        for {
          fromList   <- List("1", "2", "3").validateAs[NonEmptyChain[Int]]
          fromVector <- Vector("1", "2", "3").validateAs[NonEmptyChain[Int]]
        } yield {
          assertTrue(
            fromList == NonEmptyChain.of(1, 2, 3),
            fromVector == NonEmptyChain.of(1, 2, 3),
          )
        }
      }
      test("fails with the Required violation when the collection is empty") {
        assertTrue(
          List.empty[String].validateAs[NonEmptyChain[Int]].is(_.left) == Violations.of(Violation.Required),
        )
      }
      test("reports the violation under the index of the invalid element") {
        assertTrue(
          List("1", "abc").validateAs[NonEmptyChain[Int]].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild(1),
        )
      }
      test("accumulates the violations of every invalid element") {
        assertTrue(
          List("abc", "2", "def").validateAs[NonEmptyChain[Int]].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild(0) ++
            Violations.of(Violation.NonIntegerString("def")).asChild(2),
        )
      }
    }
    suiteAll("NonEmptyChain → NonEmptyChain") {
      test("transforms every element") {
        for {
          result <- NonEmptyChain.of("1", "2", "3").validateAs[NonEmptyChain[Int]]
        } yield {
          assertTrue(result == NonEmptyChain.of(1, 2, 3))
        }
      }
      test("reports the violation under the index of the invalid element") {
        assertTrue(
          NonEmptyChain.of("1", "abc").validateAs[NonEmptyChain[Int]].is(_.left) ==
            Violations.of(Violation.NonIntegerString("abc")).asChild(1),
        )
      }
    }
  }
}
