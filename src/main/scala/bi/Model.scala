package bi

import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import java.sql.Timestamp
import org.squeryl.KeyedEntity
import org.squeryl.PersistenceStatus

// helpers
sealed abstract class Sex
case object Male extends Sex
case object Female extends Sex

sealed abstract class PersonType
case object Student extends PersonType
case object Tutor extends PersonType
case object Lecturer extends PersonType

// General
case class Person(
  id: String,
  name: String,
  email: String,
  geschlecht: Option[Sex],
  dateOfBirth: Option[LocalDate],
  personType: PersonType)

case class Lva(name: String, semester: Semester)

case class Semester(val id: Int)

// Forum
case class Forum(id: Long, langtext: String, lva_dbid: Long) extends KeyedEntity[Long]

/*
CREATE TABLE `forumsbereich` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bereichsid` int(11) NOT NULL,
  `name` varchar(64) DEFAULT NULL,
  `beschreibung` varchar(256) DEFAULT NULL,
  `forum_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `forumsbereich_unique` (`bereichsid`,`forum_id`),
  KEY `forumsbereich_forum_idx` (`forum_id`),
  CONSTRAINT `forumsbereich_forum` FOREIGN KEY (`forum_id`) REFERENCES `forum` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1
*/

case class Forumsbereich(id: Long, bereichsid: Long, name: String, beschreibung: String, forum_id: Long) extends KeyedEntity[Long]

object Forumsbereich {
  private val header = s"id${s}name"
}

case class Posting(id: Long, postingid: Long, datum: Timestamp, betreff_laenge: Int, text_laenge: Int, forumsbereich_id: Long, parent_id: Option[Long]) extends KeyedEntity[Long]

case class CodeTopic(id: Long, name: String, lva_dbid: Int) extends KeyedEntity[Long]

case class CodeUpload(id: Long, zeitstempel: Timestamp, user_id: String, code_topic_id: Int) extends KeyedEntity[Long]

object MySchema extends Schema {
  val tCodeTopic = table[CodeTopic]("code_topic")
  on(tCodeTopic)(g => declare(
    g.id is (primaryKey)))

  val tCodeUpload = table[CodeUpload]("code_upload")
  on(tCodeUpload)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tForum = table[Forum]("forum")
  on(tForum)(g => declare(
    g.id is (primaryKey)))

  val tForumsbereich = table[Forumsbereich]("forumsbereich")
  on(tForumsbereich)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tPosting = table[Posting]("posting")
  on(tPosting)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  //  // ManyToMany Event - Membership
  //  class EventMembership(val eventId: Long, val membershipId: Long) extends KeyedEntity[CompositeKey2[Long, Long]] {
  //    override def id = compositeKey(eventId, membershipId)
  //  }
  //  val eventsMemberships =
  //    manyToManyRelation(tEvent, tMembership, "event_membership").
  //      via[EventMembership]((event, membership, cs) => (cs.eventId === event.id, membership.id === cs.membershipId))
  //
  //  // ManyToMany User - Games
  //  class UserGame(val userId: Long, val gameId: Long, var signedIn: SignedInEnum.SignedInEnum, var comment: String) extends KeyedEntity[CompositeKey2[Long, Long]] {
  //    //    def this=this(0,0,0,SignedInEnum.Unknown)
  //    def this() = this(0L, 0L, SignedInEnum.Unknown, "")
  //
  //    override def id = compositeKey(userId, gameId)
  //    def signedInEnum = signedIn
  //    override def toString = userId + "/" + gameId + "/" + signedInEnum
  //  }
  //  val usersGames =
  //    manyToManyRelation(tUser, tGame, "user_game").
  //      via[UserGame]((user, game, cs) => (cs.userId === user.id, game.id === cs.gameId))
  //
  //  // ManyToMany User - Memberships
  //  class UserMembership(val userId: Long, val membershipId: Long) extends KeyedEntity[CompositeKey2[Long, Long]] {
  //    override def id = compositeKey(userId, membershipId)
  //  }
  //  val usersMemberships =
  //    manyToManyRelation(tUser, tMembership, "user_membership").
  //      via[UserMembership]((user, membership, cs) => (cs.userId === user.id, membership.id === cs.membershipId))
  //
  //  //   OneToMany Membership - Game
  //  val gameMemberships = oneToManyRelation(tMembership, tGame).via((m, g) => g.membershipId === m.id)
  //
  //  //   OneToMany User - Game (launderer)
  //  val gameLaunderers = oneToManyRelation(tUser, tGame).via((u, g) => g.laundererId === u.id)
  //
  //  // Joins
  //  def getEventsMemberships =
  //    from(tEvent, tMembership, eventsMemberships)((e, m, em) =>
  //      where(e.id === em.eventId and m.id === em.membershipId)
  //        select (e, m) orderBy (e.start))
  //  def getEventsMemberships(eventsAfter: Timestamp) =
  //    from(tEvent, tMembership, eventsMemberships)((e, m, em) =>
  //      where(e.id === em.eventId and m.id === em.membershipId and (e.start >= eventsAfter))
  //        select (e, m) orderBy (e.start))
  //
  //  def getEvent(id: Long) = tEvent.where(_.id === id).single
  //
  //  def getEventsWithMemberships = getEventsMemberships.asTree.map { e => e._1.memberships2 = e._2.map(_.name); e._1 }.toSeq.sortBy(_.start.getTime)
  //
  //  def getEventsWithMemberships(eventsAfter: Timestamp) = getEventsMemberships(eventsAfter).asTree.map { e => e._1.memberships2 = e._2.map(_.name); e._1 }.toSeq.sortBy(_.start.getTime)
  //
  //
  //  def allUsers = from(tUser)(select(_))
  //
  //  val allTables = Seq(usersMemberships, usersGames, tGame, tUser, eventsMemberships, tEvent, newsMemberships, tNews, tMembership)
}
