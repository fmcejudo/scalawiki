package org.scalawiki.wlx

import org.scalawiki.wlx.dto.lists.ListConfig.WleUa
import org.scalawiki.wlx.dto.{Contest, Country, Monument}
import org.specs2.mutable.Specification

class MonumentDbSpec extends Specification {

  private val Ukraine = Country.Ukraine

  val monuments = Ukraine.regionIds.flatMap{
    regionId =>
      (1 to regionId.toInt).map { i =>
        Monument(
          page = "",
          id = regionId + "-001-" + f"$i%04d",
          name = "Monument in " +  Ukraine.regionName(regionId),
          listConfig = Some(WleUa)
        )
      }
  }

   "monument db" should {
     "contain monuments ids" in {
       val contest = Contest.WLMUkraine(2014)

       val db = new MonumentDB(contest, monuments.toSeq)

       db.ids.size === monuments.size
       db.ids ===  monuments.map(_.id)
     }

    "group monuments by regions" in {
      val contest = Contest.WLMUkraine(2014)

      val db = new MonumentDB(contest, monuments.toSeq)

      val regions = db._byRegion.keySet

      for (region <- regions) yield {
        db._byRegion(region).size === region.toInt
      }

      regions === Ukraine.regionIds
    }
  }

}
