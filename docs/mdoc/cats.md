---
id: cats
title: cats
sidebar_position: 3
---

# cats

`yoshi-cats` lets a validation target [cats](https://typelevel.org/cats/) data types, and lets its result flow into the cats combinators you already use.

## Setup

```scala
libraryDependencies += "dev.hshn" %% "yoshi-cats" % "@VERSION@"
```

Having the module on the classpath is enough — `import yoshi.*` brings its instances in along with the core syntax. Nothing else to import.

```scala mdoc:silent
import yoshi.*
import yoshi.defaults.*
```

## Non-empty collections

A `NonEmptyList[A]` validates element by element, and each violation is reported under the index of the element it came from:

```scala mdoc:silent
import cats.data.NonEmptyList
```

```scala mdoc
NonEmptyList.of("1", "x", "3").validateAs[NonEmptyList[Int]].left.map(_.toList)
```

Any `Iterable` can be validated into a `NonEmptyList[B]` too. The output type says the domain needs at least one element, so an empty input fails with the `Required` violation — the same one an absent `Option` reports:

```scala mdoc
List("1", "2").validateAs[NonEmptyList[Int]]
List.empty[String].validateAs[NonEmptyList[Int]].left.map(_.toList)
```

Inside a cursor this reads like any other field:

```scala mdoc:silent
case class FormInput(tags: List[String])
case class Post(tags: NonEmptyList[Int])

val post: Validation[Violation, FormInput, Post] =
  Validation.cursor[FormInput] { c =>
    (
      c.validateAs[NonEmptyList[Int]](_.tags)
    ).validateN { tags =>
      Post(tags)
    }
  }
```

```scala mdoc
post.run(FormInput(tags = List("1", "x"))).left.map(_.toList)
post.run(FormInput(tags = Nil)).left.map(_.toList)
```

`NonEmptyChain` and `NonEmptySet` work the same way — each validates element by element, and any `Iterable` validates into one, failing with `Required` when empty. The index in a violation path follows the input's iteration order, so validate from an ordered collection where the path has to be stable.

As with the core derivations, the module knows nothing about your violation type. It reports an empty input through the `Required` instance — `yoshi.defaults` supplies one for `Violation`, and a violation type of your own needs a single `given Required[MyViolation]`, as described in [Getting Started](./index.md#required-fields).

## Validated

`runValidated` runs a validation into `Validated`, so its result composes with the accumulating cats combinators:

```scala mdoc:silent
import cats.syntax.all.*

case class Member(name: String, age: Int)
```

```scala mdoc
(
  Validations.minLength(3).runValidated("ab").leftMap(_.asChild("name")),
  Validations.parseInt.runValidated("abc").leftMap(_.asChild("age")),
).mapN(Member.apply).leftMap(_.toList)
```

The other direction is `Validation.fromValidated`: a function that reports failures as `Validated` becomes an ordinary `Validation`, composable with everything else.

```scala mdoc:silent
import cats.data.Validated

val parseInt: Validation[Violation, String, Int] =
  Validation.fromValidated { value =>
    value.toIntOption match
      case Some(int) => Validated.valid(int)
      case None      => Validated.invalid(Violations.of(Violation.NonIntegerString(value)))
  }

val requiredInt: Validation[Violation, Option[String], Int] =
  Validations.required[String] >> parseInt
```

```scala mdoc
requiredInt.run(Some("42"))
requiredInt.run(None).left.map(_.toList)
```

## Violations as a Monoid

`Violations[V]` has a `Monoid` that merges violation trees. This is what `Validated` needs from its error type to accumulate through `mapN` above, and it is what lets `Either[Violations[V], *]` — the type every `run` returns — accumulate through `parMapN`:

```scala mdoc
(
  Option.empty[String].validateAs[String].at("name"),
  "abc".validateAs[Int].at("age"),
).parMapN((_, _)).left.map(_.toList)
```
