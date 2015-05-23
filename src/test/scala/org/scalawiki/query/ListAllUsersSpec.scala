package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.dto.User
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.dto.cmd.query.list.{AllUsers, ListParam}
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ListAllUsersSpec extends Specification with MockBotSpec {

  "get all users without continue" should {
    "return users in" in {
      val queryType = "allusers"

      val response1 =
        """{ "query": {
          |        "allusers": [
          |            {
          |                "userid": 146308,
          |                "name": "!"
          |            },
          |            {
          |                "userid": 480659,
          |                "name": "! !"
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val commands = Seq(
        new Command(Map("action" -> "query", "list" -> queryType, "continue" -> ""), response1)
      )

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              AllUsers()
            )
          )
        )

      val future = new DslQuery(action, bot).run()

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 2
      val users = result.flatMap(_.revisions.head.user)
      users(0) === User(146308, "!")
      users(1) === User(480659, "! !")
    }
  }

  "get all users with continue" should {
    "return users in" in {
      val queryType = "allusers"

      val response1 =
        """{  "continue": {
          |        "aufrom": "! ! !",
          |        "continue": "-||"
          |    },
          |    "query": {
          |        "allusers": [
          |            {
          |                "userid": 146308,
          |                "name": "!"
          |            },
          |            {
          |                "userid": 480659,
          |                "name": "! !"
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val response2 =
        """{ "query": {
          |        "allusers": [
          |             {
          |                "userid": 505506,
          |                "name": "! ! !"
          |            },
          |            {
          |                "userid": 553517,
          |                "name": "! ! ! !"
          |            }
          |         ]
          |    }
          |}""".stripMargin

      val commands = Seq(
        new Command(Map("action" -> "query", "list" -> queryType, "continue" -> ""), response1),
        new Command(Map("action" -> "query", "list" -> queryType,
          "aufrom" -> "! ! !", "continue" -> "-||"), response2)
      )

      val bot = getBot(commands: _*)

      val action =
        Action(
          Query(
            ListParam(
              AllUsers()
            )
          )
        )

      val future = new DslQuery(action, bot).run()

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))
      result must have size 4
      val users = result.flatMap(_.revisions.head.user)
      users(0) === User(146308, "!")
      users(1) === User(480659, "! !")
      users(2) === User(505506, "! ! !")
      users(3) === User(553517, "! ! ! !")
    }
  }
}