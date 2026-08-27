package yoshi.defaults

import yoshi.Required
import yoshi.Validation

implicit val requiredViolation: Required[Violation] =
  Required(Violation.Required)

implicit val stringCanBeInt: Validation[Violation, String, Int] =
  Validations.parseInt
