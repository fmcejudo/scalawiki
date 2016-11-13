package org.scalawiki.dto.cmd.action

import org.scalawiki.dto.cmd.parse.Parse
import org.scalawiki.dto.cmd.{EnumArg, EnumArgument, EnumParameter}
import org.scalawiki.dto.cmd.query.Query

/**
  * Created by francisco on 12/11/16.
  */
trait ActionArg extends EnumArg[ActionArg] {
  /*val param = ActionParam*/
}

abstract class Action[T](override val arg: ActionArg) extends EnumParameter[ActionArg]("action", "") {
  def query: Option[T]
  override def toString = pairs.toString()
}

case class QueryAction(override val arg: ActionArg) extends Action[Query](arg){
  def query: Option[Query] = args.collect { case q: Query => q }.headOption
}

case class ParseAction(override val arg: ActionArg) extends Action[Parse](arg){
  def query: Option[Parse] = args.collect { case p: Parse => p }.headOption
}