package org.scalawiki.dto.cmd.action

import org.scalawiki.dto.cmd.{EnumArg, EnumParameter}
import org.scalawiki.dto.cmd.query.Query

/**
  * Created by francisco on 12/11/16.
  */
trait ActionArg extends EnumArg[ActionArg] {
  /*val param = ActionParam*/
}

case class Action(override val arg: ActionArg) extends EnumParameter[ActionArg]("action", "") {
  def query: Option[Query] = args.collect { case q: Query => q }.headOption
  override def toString = pairs.toString()
}

case class PageAction(){

}

case class ParseAction(){

}