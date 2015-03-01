package org.scalawiki.dto.history

import org.joda.time.DateTime
import org.scalawiki.dto.Page.Id
import org.scalawiki.dto.Revision

class RevisionFilter(
                       val from: Option[DateTime] = None,
                       val to: Option[DateTime] = None,
                       val user: Option[String] = None,
                       val userId: Option[Id] = None) {

  def apply(revisions: Seq[Revision]): Seq[Revision] = {
    revisions.filter(predicate)
  }

  def predicate(rev: Revision):  Boolean = {
        rev.timestamp.forall(ts => from.forall(ts.isAfter) && to.forall(ts.isBefore)) &&
        rev.user.forall(u => user.forall(u.equals)) &&
        rev.userId.forall(u => userId.forall(u.equals))
  }
}