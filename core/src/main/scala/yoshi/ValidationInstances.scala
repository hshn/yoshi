package yoshi

trait ValidationInstances extends ValidationInstancesLowPriority {

  /** Automatically lifts a `Validation[V, A, B]` to `Validation[V, Option[A], Option[B]]`. `None` passes through; `Some(a)` is validated.
    */
  given optionCanBeValidatedAs[V, A, B](using validation: Validation[V, A, B]): Validation[V, Option[A], Option[B]] =
    validation.optional

  /** Automatically validates each element of a `Seq`, accumulating violations by index. */
  given seqCanBeValidatedAs[V, A, B](using validation: Validation[V, A, B]): Validation[V, Seq[A], Seq[B]] =
    Validation.instance { values =>
      val results = values.toList.zipWithIndex.map { case (a, index) =>
        validation.run(a).left.map(_.asChild(Violations.Path.Index(index)))
      }
      val (errors, successes) = results.partitionMap(identity)
      if (errors.isEmpty) Right(successes)
      else Left(errors.reduce(_ ++ _))
    }

  /** Automatically validates each element of a `List`, accumulating violations by index. */
  given listCanBeValidatedAs[V, A, B](using Validation[V, A, B]): Validation[V, List[A], List[B]] =
    seqCanBeValidatedAs[V, A, B].contramap[List[A]](identity).map(_.toList)

  /** Automatically validates each value of a `Map[String, A]`, accumulating violations by key. */
  given mapCanBeValidatedAs[V, A, B](using
    validation: Validation[V, A, B],
  ): Validation[V, Map[String, A], Map[String, B]] =
    Validation.instance { values =>
      val results = values.toList.map { case (key, a) =>
        validation.run(a).left.map(_.asChild(Violations.Path.Key(key))).map(b => key -> b)
      }
      val (errors, successes) = results.partitionMap(identity)
      if (errors.isEmpty) Right(successes.toMap)
      else Left(errors.reduce(_ ++ _))
    }
}

trait ValidationInstancesLowPriority extends ValidationInstancesLowestPriority {

  /** Treats an `Option[A]` as a required value and validates what it holds into `B`: `None` fails with the violation from [[Required]],
    * `Some(a)` is validated by the `Validation[V, A, B]` in scope.
    *
    * Whether a field is required is expressed by the type it is validated into. `validateAs[B]` demands a value, while
    * `validateAs[Option[B]]` accepts its absence and passes `None` through.
    */
  given optionCanBeValidatedAsRequired[V, A, B](using
    validation: Validation[V, A, B],
    required: Required[V],
  ): Validation[V, Option[A], B] =
    Validation.required[V, A](required.violation) >> validation
}

trait ValidationInstancesLowestPriority {

  /** Leaves a value unchanged, so a derivation whose output type matches its input needs no instance of its own.
    *
    * `Option[A]` to `A` and `Seq[A]` to `Seq[A]` fall out of the derivations above through this instance. A `Validation[V, A, A]` the
    * caller defines or imports is in lexical scope and takes precedence over this one, so it is still applied where it exists.
    */
  given valueCanBeValidatedAsItself[V, A]: Validation[V, A, A] =
    Validation.succeed[A]
}
