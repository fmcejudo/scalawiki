package org.scalawiki.dto.cmd.parse

import org.scalawiki.dto.cmd._
import org.scalawiki.dto.cmd.action.ActionArg
import org.scalawiki.dto.cmd.query.prop.PropArg

/**
  * Created by francisco on 29/10/16.
  *
  * ? action=parse
  */
case class Parse(override val params: ParseParams[Any]*)
  extends EnumArgument[ActionArg]("parse", "Retrieve definition of wiki")
    with ActionArg with ArgWithParams[ParseParams[Any], ActionArg] {

  val props : Seq[PropArg] = Seq()

}


trait ParseParams[+T] extends Parameter[T]

//This only really matters when parsing links to the page itself or subpages, or when using magic words like {{PAGENAME}}.
case class ParseTitleParam(override val arg: String)
  extends StringParameter("title", "Act like the wikitext is on this page") with ParseParams[String]


//this parameter can be use with title and text
case class ParsePageParam(override val arg: String)
  extends StringParameter("page", "Parse the content of this page") with ParseParams[String]


case class ParsePageIdParam(override val arg: Int)
  extends IntParameter("pageid", "Parse the content of this page. Override page") with ParseParams[Int]
