package yoshi.defaults

import yoshi.Required
import yoshi.Validation

implicit val optionIsRequired: Required[Violation] =
  Required(Violation.Required)

implicit val stringCanBeInt: Validation[Violation, String, Int] =
  Validations.parseInt
