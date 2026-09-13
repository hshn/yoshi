---
id: zio-prelude
title: zio-prelude
sidebar_position: 2
---

# zio-prelude

`yoshi-zio-prelude` lets a validation target [zio-prelude](https://github.com/zio/zio-prelude) types: its non-empty collections become validation outputs, and a `Newtype` becomes a validation.

## Setup

```scala
libraryDependencies += "dev.hshn" %% "yoshi-zio-prelude" % "@VERSION@"
```

Having the module on the classpath is enough — `import yoshi.*` brings its instances in along with the core syntax. Nothing else to import.

```scala mdoc:silent
import yoshi.*
import yoshi.defaults.*
```

## Non-empty collections

A `NonEmptyList[A]` validates element by element, and each violation is reported under the index of the element it came from:

```scala mdoc:silent
import zio.prelude.NonEmptyList
```

```scala mdoc
NonEmptyList("1", "x", "3").validateAs[NonEmptyList[Int]].left.map(_.toList)
```

A plain `List[A]` can be validated into a `NonEmptyList[B]` too. The output type says the domain needs at least one element, so an empty input fails with the `Required` violation — the same one an absent `Option` reports:

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

`NonEmptySet` works the same way: `NonEmptySet[A]` validates into `NonEmptySet[B]`, and a `Set[A]` validates into `NonEmptySet[B]`, failing with `Required` when empty.

As with the core derivations, the module knows nothing about your violation type. It reports an empty input through the `Required` instance — `yoshi.defaults` supplies one for `Violation`, and a violation type of your own needs a single `given Required[MyViolation]`, as described in [Getting Started](./index.md#required-fields).

## Newtypes

`Validation.newtype` turns a `Newtype` into a validation from the underlying type to the newtype, using the newtype's own assertion. You decide what an assertion failure becomes: the function receives the rejected value and the assertion's message.

```scala mdoc:silent
import zio.prelude.Assertion
import zio.prelude.Newtype

object PositiveInt extends Newtype[Int]:
  override inline def assertion: Assertion[Int] = Assertion.greaterThan(0)
type PositiveInt = PositiveInt.Type

val positiveInt: Validation[Violation, Int, PositiveInt] =
  Validation.newtype(PositiveInt)((value, _) => Violation.NonPositive(value))
```

```scala mdoc
positiveInt.run(42)
positiveInt.run(0).left.map(_.toList)
```

Being an ordinary `Validation`, it composes with everything else — parse first, then refine:

```scala mdoc:silent
val parsePositiveInt: Validation[Violation, String, PositiveInt] =
  Validations.parseInt >> positiveInt
```

```scala mdoc
parsePositiveInt.run("42")
parsePositiveInt.run("abc").left.map(_.toList)
```

## Accumulating with zio-prelude

The module also provides `AssociativeBoth` for `Either[Violations[V], *]`, so zio-prelude's own combinators accumulate violations instead of stopping at the first failure:

```scala mdoc:silent
import zio.prelude.AssociativeBothOps
```

```scala mdoc
(
  Option.empty[String].validateAs[String].at("name") <*>
    "abc".validateAs[Int].at("age")
).left.map(_.toList)
```
