package org.scalawiki.bots

import org.joda.time.{DateTime, DateTimeZone}
import org.scalawiki.MwBot
import org.scalawiki.dto.cmd.action.QueryAction
import org.scalawiki.dto.cmd.query.list.{CategoryMembers, CmLimit, CmNamespace, CmTitle}
import org.scalawiki.dto.cmd.query.prop._
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query}
import org.scalawiki.dto.{Namespace, Page}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Spain {

  var bot: MwBot = _

  def main(args: Array[String]) {
    bot = MwBot.fromHost(MwBot.commons)

    val action = QueryAction(Query(
      Prop(
        Info(),
        ImageInfo(
//          IiProp(Timestamp)
        )
        //        Revisions(
        //          RvProp(Ids)
        //          //,RvDir(Newer)
        //        )
      ),
      Generator(CategoryMembers(
        CmTitle("Category:Evaluation of images from Wiki Loves Earth 2015 in Spain and Portugal - Round 1"),
        CmLimit("500"),
        CmNamespace(Seq(Namespace.FILE))))
    ))

    bot.run(action) map {
      pages =>
        val filteredPages = pages.filterNot { page =>
          val last = page.images.last
          last.date.exists {
            timestamp =>
              val start = new DateTime(2015, 4, 30, 22, 0, DateTimeZone.UTC)
              val end = new DateTime(2015, 5, 31, 23, 0, DateTimeZone.UTC)
              timestamp.isAfter(start) && timestamp.isBefore(end)
          }
        }
        val size = filteredPages.size
        filteredPages.foreach(p => println(p.title))
    }


    //    new DslQueryDbCache(new DslQuery(action, bot)).run() map {
    //      pages =>
    //        Future.traverse(pages.sliding(50, 50)) {
    //          window =>
    //            pagesRevisions(window.flatMap(_.id))
    //        }.map {
    //          withRevs =>
    //            withRevs.flatten
    //        }.map {
    //          flatPages =>
    //            flatPages.filterNot { page =>
    //              val last = page.revisions.last
    //              last.timestamp.exists{
    //                timestamp =>
    //                  val start = new DateTime(2015, 4, 30, 22, 0, DateTimeZone.UTC)
    //                  val end = new DateTime(2015, 5, 31, 22, 59, DateTimeZone.UTC)
    //                  timestamp.isBefore(start) && timestamp.isAfter(end)
    //              } && last.parentId.forall(_ == 0)
    //            }
    //        }
    //  }
  }


  def pagesRevisions(ids: Seq[Long]): Future[Seq[Page]] = {
    Future.traverse(ids)(pageRevisions).map(_.flatten)
  }

  def pageRevisions(id: Long): Future[Option[Page]] = {
    import org.scalawiki.dto.cmd.query.prop.rvprop._

    val action = QueryAction(Query(
      PageIdsParam(Seq(id)),
      Prop(
        Info(),
        Revisions(
          RvProp(/*Content,*/ Ids, Size, User, UserId, rvprop.Timestamp),
          RvLimit("max")
        )
      )
    ))

    bot.run(action).map (_.headOption)
  }
}
