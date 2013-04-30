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
case class Forum(id: Int, lva: Lva, bereiche: Seq[Forumsbereich])

case class Forumsbereich(id: Int, name: String, postings: Seq[Posting]) 

object Forumsbereich {
  private val header = s"id${s}name"
}

case class Posting(id: Int, datum: LocalDateTime, betreffLaenge: Int, textLaenge: Int, sub: Seq[Posting], parent: Option[Posting]) 

object Posting {
  private val header = s"id${s}datum${s}betreffLänge${s}textLänge${s}parent"
}
trait MyKeyedEntity[K] extends PersistenceStatus {

  def dbid: K

  override def hashCode =
    if(isPersisted)
      dbid.hashCode
    else
      super.hashCode

  override def equals(z: Any):Boolean = {
    if(z == null)
      return false
    val ar = z.asInstanceOf[AnyRef]
    if(!ar.getClass.isAssignableFrom(this.getClass))
      false
    else if(isPersisted)
      dbid == ar.asInstanceOf[MyKeyedEntity[K]].dbid
    else
      super.equals(z)
  }
}
case class CodeTopic(dbid:Long, id:Int, name:String, lva_dbid:Int) extends MyKeyedEntity[Long]

case class CodeUpload(dbid:Long, zeitstempel:Timestamp, user_id:Int, code_topic_id:Int) extends MyKeyedEntity[Long]

object MySchema extends Schema {
  val tCodeTopic = table[CodeTopic]("code_topic")
  on(tCodeTopic)(g => declare(
    g.dbid is (primaryKey, autoIncremented)))

  val tCodeUpload = table[CodeUpload]("code_upload")
  on(tCodeUpload)(g => declare(
    g.dbid is (primaryKey, autoIncremented)))


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
