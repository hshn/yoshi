---
id: zio
title: zio
sidebar_position: 2
---

# zio

`yoshi-zio` lets a validation target [zio](https://zio.dev/) core types: a `Chunk` validates into a `NonEmptyChunk`, and a `NonEmptyChunk` validates element by element.

## Setup

```scala
libraryDependencies += "dev.hshn" %% "yoshi-zio" % "@VERSION@"
```

Having the module on the classpath is enough — `import yoshi.*` brings its instances in along with the core syntax. Nothing else to import.

```scala mdoc:silent
import yoshi.*
import yoshi.defaults.*
```

## Chunks

A `NonEmptyChunk[A]` validates element by element, and each violation is reported under the index of the element it came from:

```scala mdoc:silent
import zio.Chunk
import zio.NonEmptyChunk
```

```scala mdoc
NonEmptyChunk("1", "x", "3").validateAs[NonEmptyChunk[Int]].left.map(_.toList)
```

A `Chunk[A]` can be validated into a `NonEmptyChunk[B]` too. The output type says the domain needs at least one element, so an empty input fails with the `Required` violation — the same one an absent `Option` reports:

```scala mdoc
Chunk("1", "2").validateAs[NonEmptyChunk[Int]]
Chunk.empty[String].validateAs[NonEmptyChunk[Int]].left.map(_.toList)
```

Inside a cursor this reads like any other field:

```scala mdoc:silent
case class FormInput(tags: Chunk[String])
case class Post(tags: NonEmptyChunk[Int])

val post: Validation[Violation, FormInput, Post] =
  Validation.cursor[FormInput] { c =>
    (
      c.validateAs[NonEmptyChunk[Int]](_.tags)
    ).validateN { tags =>
      Post(tags)
    }
  }
```

```scala mdoc
post.run(FormInput(tags = Chunk("1", "x"))).left.map(_.toList)
post.run(FormInput(tags = Chunk.empty)).left.map(_.toList)
```

As with the core derivations, the module knows nothing about your violation type. It reports an empty input through the `Required` instance — `yoshi.defaults` supplies one for `Violation`, and a violation type of your own needs a single `given Required[MyViolation]`, as described in [Getting Started](./index.md#required-fields).
