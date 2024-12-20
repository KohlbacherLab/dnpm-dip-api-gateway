package de.dnpm.dip.rest.api


import de.dnpm.dip.rest.util.Collection
import de.dnpm.dip.rest.util.sapphyre.{
  Operation,
  Link,
  Relations,
  Method,
  Hyper,
  HypermediaBase
}
import de.dnpm.dip.service.query.{
  PatientMatch,
  Query,
  PreparedQuery,
  UseCaseConfig
}
import de.dnpm.dip.service.validation.{
  ValidationInfo,
  ValidationReport
}
import scala.util.chaining._


/*
case class HyperEntry[+K,+V](
  key: K,
  value: V,
  children: Option[Seq[HyperEntry[K,V]]]
)
extends Hypermediable


type HyperConceptCount[+T] = HyperEntry[T,Count]


case class HyperDistribution[T](
  total: Int,
  elements: Seq[HyperConceptCount[T]]
)
extends Hypermediable
*/


trait UseCaseHypermedia[UseCase <: UseCaseConfig] extends HypermediaBase
{

  type QueryType = Query[UseCase#Criteria]

  type PreparedQueryType = PreparedQuery[UseCase#Criteria]


  import Hyper.syntax._
  import Relations.{
    COLLECTION,
    SELF,
    CREATE,
    UPDATE
  }
  import Method.{DELETE,PATCH,POST,PUT}



  val prefix: String


  protected val BASE_URI =
    s"$BASE_URL/$prefix"

  private val VALIDATION_BASE_URI =
    s"$BASE_URI/validation"

  private val QUERY_BASE_URI =
    s"$BASE_URI/queries"

  private val PREPARED_QUERY_BASE_URI =
    s"$BASE_URI/prepared-queries"


  protected def QueryUri(id: Query.Id) =
    s"$QUERY_BASE_URI/${id.value}"

  protected def Uri(query: QueryType) =
    QueryUri(query.id)

  protected def PreparedQueryUri(id: PreparedQuery.Id) =
    s"$PREPARED_QUERY_BASE_URI/${id.value}"

  protected def Uri(query: PreparedQueryType) =
    PreparedQueryUri(query.id)


  implicit def HyperDataValidationInfo: Hyper.Mapper[ValidationInfo] =
    info => info.withLinks(
      COLLECTION          -> Link(s"$VALIDATION_BASE_URI"),
      "validation-report" -> Link(s"$VALIDATION_BASE_URI/report/${info.id}"),
      "patient-record"    -> Link(s"$VALIDATION_BASE_URI/patient-record/${info.id}")
    ) 


  implicit def HyperValidationReport: Hyper.Mapper[ValidationReport] =
    report => report.withLinks(
      "infos"             -> Link(s"$VALIDATION_BASE_URI/infos"),
      "patient-record"    -> Link(s"$VALIDATION_BASE_URI/patient-record/${report.patient}")
    ) 


  implicit def HyperQuery: Hyper.Mapper[QueryType] = {
    query =>

      val selfLink =
        Link(Uri(query))

      query.withLinks(
        SELF              -> selfLink,
        "demographics"    -> Link(s"${Uri(query)}/demographics"),
        "patient-matches" -> Link(s"${Uri(query)}/patient-matches"),
      )
      .withOperations(
        Relations.UPDATE -> Operation(PUT, selfLink),
        Relations.DELETE -> Operation(DELETE, selfLink)
      )
  }


  implicit def HyperFilter(
    implicit id: Query.Id
  ): Hyper.Mapper[UseCase#Filter] =
    _.withLinks(
      SELF    -> Link(s"${QueryUri(id)}/filters"),
      "query" -> Link(QueryUri(id))
    )


  implicit def HyperQueries: Hyper.Mapper[Collection[Hyper[QueryType]]] =
    _.withLinks(
       SELF -> Link(QUERY_BASE_URI)
     )
     .withOperations(
       "submit" -> Operation(POST, Link(QUERY_BASE_URI))
     )



  implicit def HyperPatientMatch(
    implicit id: Query.Id
  ): Hyper.Mapper[PatientMatch[UseCase#Criteria]] =
    patMatch => patMatch.withLinks(
      "query"          -> Link(QueryUri(id)),
      "patient-record" -> Link(s"${QueryUri(id)}/patient-record/${patMatch.id.value}")
    )


  implicit def HyperPatientMatches(
    implicit id: Query.Id,
  ): Hyper.Mapper[Collection[Hyper[PatientMatch[UseCase#Criteria]]]] = {
    matches =>

      val prev =
        matches.limit.flatMap(
          n => 
            matches.offset.map(_ - n)
              .collect {
                case off if (off > 0) => off -> n
                case _                => 0 -> n
              }
        )

      val next =
        matches.limit.collect {
          case n if (matches.size - n >= 0) =>
            (matches.offset.getOrElse(0) + n -> n)
        }

    matches.withLinks(
      "query" -> Link(QueryUri(id)),
     )
     .pipe { coll =>

       prev match {
         case Some(0 -> n) =>
           coll.addLinks(
             "prev" -> Link(s"${QueryUri(id)}/patient-matches?limit=${n}"),
           )
         case Some(off -> n) =>
           coll.addLinks(
             "prev" -> Link(s"${QueryUri(id)}/patient-matches?offset=${off}&limit=${n}")
           )
         case _ => coll
       }
     }
     .pipe { coll =>

       next match {
         case Some(off -> n) =>
           coll.addLinks(
             "next" -> Link(s"${QueryUri(id)}/patient-matches?offset=${off}&limit=${n}"),
           )
         case _ => coll
       }
     }
  }


  implicit def HyperPatientRecord(
    implicit id: Query.Id
  ): Hyper.Mapper[UseCase#PatientRecord] = 
    patRec =>
      patRec.withLinks(
        "query" -> Link(QueryUri(id)),
        SELF    -> Link(s"${QueryUri(id)}/patient-record/${patRec.patient.id.value}")
      )


  implicit val HyperPreparedQuery: Hyper.Mapper[PreparedQueryType] = {
    query =>

      val selfLink =
        Link(Uri(query))

      query.withLinks(
        SELF -> selfLink,
      )
      .withOperations(
        UPDATE           -> Operation(PATCH, selfLink),
        Relations.DELETE -> Operation(DELETE, selfLink)
      )
  }


  implicit val HyperPreparedQueries: Hyper.Mapper[Collection[Hyper[PreparedQueryType]]] =
    _.withLinks(
       SELF -> Link(PREPARED_QUERY_BASE_URI)
     )
     .withOperations(
       CREATE -> Operation(POST, Link(PREPARED_QUERY_BASE_URI))
     )


}
