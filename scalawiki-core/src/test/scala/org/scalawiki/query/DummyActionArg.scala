package org.scalawiki.query

import org.scalawiki.dto.cmd.action.ActionArg

object DummyActionArg extends ActionArg {
  override val name: String = "name"
  override val summary: String = "summary"
}
