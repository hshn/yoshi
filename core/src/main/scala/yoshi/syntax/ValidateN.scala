package yoshi.syntax

import yoshi.Violations

/** Provides the steps that continue from validation results: `.validateN(f)`, `.validateWith(f)` and `.andValidateAs[B]`.
  *
  * Each step takes either a single `Either` or a tuple of them under the same name, so code keeps compiling as fields are added or removed.
  * A tuple first accumulates the violations of every element; the step runs only once all of them succeed.
  *
  * {{{
  * (
  *   nameValidation.run(input.name).at("name"),
  *   ageValidation.run(input.age).at("age"),
  * ).validateN { case (name, age) => User(name, age) }
  * }}}
  */
trait ValidateN:
  extension [V, T, Out](results: T)(using acc: Accumulate[V, T, Out])
    def validateN[A](f: Out => A): Either[Violations[V], A] =
      acc.accumulate(results).map(f)

    /** Build the result with a function that can fail in turn.
      *
      * Violations reported by `f` describe the combination itself — a rule that no single element can decide.
      *
      * {{{
      * (
      *   input.start.validateAs[Date].at("start"),
      *   input.end.validateAs[Date].at("end"),
      * ).validateWith { case (start, end) =>
      *   if (start.isBefore(end)) Right(Period(start, end))
      *   else Left(Violations.of(Violation.EndBeforeStart).asChild("end"))
      * }
      * }}}
      */
    def validateWith[A](f: Out => Either[Violations[V], A]): Either[Violations[V], A] =
      acc.accumulate(results).flatMap(f)

    /** Continue validating the already validated value, short-circuiting on the violations already collected.
      *
      * `at` maps violations that are already there, so it belongs after the chain to cover both steps. On a tuple, the next validation
      * receives the tuple of validated values, which lets a rule across several fields live in a [[yoshi.Validation]] of its own.
      *
      * {{{
      * input.id.validateAs[String].andValidateAs[UserId].at("id")
      *
      * given Validation[Violation, (Date, Date), Period] = ???
      * (
      *   input.start.validateAs[Date].at("start"),
      *   input.end.validateAs[Date].at("end"),
      * ).andValidateAs[Period]
      * }}}
      */
    def andValidateAs[B](using va: ValidatedAs[Out, B]): Either[Violations[V | va.Err], B] =
      acc.accumulate(results).flatMap(va.run)

/** Evidence that `T` — a single validation result or a tuple of them — accumulates into one result succeeding with `Out`. */
sealed trait Accumulate[V, T, Out]:
  def accumulate(results: T): Either[Violations[V], Out]

object Accumulate:
  given single[V, A]: Accumulate[V, Either[Violations[V], A], A] with
    def accumulate(result: Either[Violations[V], A]): Either[Violations[V], A] =
      result

  given tuple[V, T <: Tuple, Out <: Tuple](using tv: ValidateTuple[V, T, Out]): Accumulate[V, T, Out] with
    def accumulate(results: T): Either[Violations[V], Out] =
      tv.validate(results)

sealed trait ValidateTuple[V, T <: Tuple, Out <: Tuple]:
  def validate(t: T): Either[Violations[V], Out]

object ValidateTuple:
  given empty[V]: ValidateTuple[V, EmptyTuple, EmptyTuple] with
    def validate(t: EmptyTuple): Either[Violations[V], EmptyTuple] =
      Right(EmptyTuple)

  given cons[V, H, T <: Tuple, TOut <: Tuple](using
    tail: ValidateTuple[V, T, TOut],
  ): ValidateTuple[V, Either[Violations[V], H] *: T, H *: TOut] with
    def validate(t: Either[Violations[V], H] *: T): Either[Violations[V], H *: TOut] =
      (t.head, tail.validate(t.tail)) match {
        case (Right(h), Right(t)) => Right(h *: t)
        case (Left(e1), Left(e2)) => Left(e1 ++ e2)
        case (Left(e), _)         => Left(e)
        case (_, Left(e))         => Left(e)
      }
