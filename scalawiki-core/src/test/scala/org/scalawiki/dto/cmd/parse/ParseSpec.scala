package org.scalawiki.dto.cmd.parse


import org.specs2.mutable.Specification


/**
  * Created by francisco on 12/11/16.
  */
class ParseSpec extends Specification {

  "Parse" should {
    "create a json with action = parse" in {
      val parse = Parse(
        ParseTitleParam("title"),
        ParsePageIdParam(2),
        ParsePageParam("page")
      )

      val expectedParam = Seq("title","pageid","page")
      parse.pairs.map(_._1).toList === expectedParam
    }
  }

}
