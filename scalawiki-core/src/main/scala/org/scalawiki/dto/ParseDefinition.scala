package org.scalawiki.dto

/**
  * Created by francisco on 29/10/16.
  */
case class ParseDefinition(
                            title: String,
                            pageId: Long,
                            sections: Seq[SectionDefinition] = Seq.empty
                          )

object ParseDefinition {

  def apply(title: String, pageId: Long) = new ParseDefinition(title, pageId)
}