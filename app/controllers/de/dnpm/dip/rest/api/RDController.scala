package de.dnpm.dip.rest.api



import javax.inject.Inject
import scala.concurrent.{
  Future,
  ExecutionContext
}
import play.api.mvc.{
  Action,
  AnyContent,
  RequestHeader,
  ControllerComponents
}
import play.api.libs.json.{
  Json,
  Format,
  Reads,
  Writes
}
import de.dnpm.dip.rest.util._
import de.dnpm.dip.util.Completer
import de.dnpm.dip.service.query.{
  PatientFilter,
  Query,
  ResultSet
}
import de.dnpm.dip.coding.Coding 
import de.dnpm.dip.rd.model.{
  HPO,
  Orphanet,
  RDDiagnosis,
  RDPatientRecord,
  Completers
}
import de.dnpm.dip.rd.validation.api.{
  RDValidationPermissions,
  RDValidationService
}
import de.dnpm.dip.rd.query.api.{
  RDConfig,
  RDFilters,
  HPOFilter,
  DiagnosisFilter,
  RDQueryPermissions,
  RDQueryService,
  RDResultSet
}
import de.dnpm.dip.auth.api.{
  Authorization,
  UserPermissions,
  UserAuthenticationService
}



class RDController @Inject()(
  override val controllerComponents: ControllerComponents,
)(
  implicit ec: ExecutionContext,
)
extends UseCaseController[RDConfig]
with ValidationAuthorizations[UserPermissions]
with QueryAuthorizations[UserPermissions]
with RDHypermedia
//with UseCaseHypermedia[RDConfig]
{

  import de.dnpm.dip.rest.util.AuthorizationConversions._


  override lazy val prefix = "rd"

  override implicit val completer: Completer[RDPatientRecord] =
    Completers.rdPatientRecordCompleter


  override val validationService: RDValidationService =
    RDValidationService.getInstance.get

  override val queryService: RDQueryService =
    RDQueryService.getInstance.get


  override val SubmitQuery =
    RDQueryPermissions.SubmitQuery

  override val ReadQueryResult =
    RDQueryPermissions.ReadResultSummary

  override val ReadPatientRecord =
    RDQueryPermissions.ReadPatientRecord

  override val ReadValidationInfos =
    RDValidationPermissions.ReadValidationInfos

  override val ReadValidationReport =
    RDValidationPermissions.ReadValidationReport

  override val ReadInvalidPatientRecord =
    RDValidationPermissions.ReadInvalidPatientRecord


  private val HPOTerms =
    Extractor.AsCodingsOf[HPO]

  private val Categories =
    Extractor.Codings[RDDiagnosis.Category]


  override def FilterFrom(
    req: RequestHeader,
  ): RDFilters = 
    RDFilters(
      PatientFilterFrom(req),
      HPOFilter(
        req.queryString.get("hpo[value]") collect {
          case HPOTerms(hpos) if hpos.nonEmpty => hpos
        }
      ),
      DiagnosisFilter(
        req.queryString.get("diagnosis[category]") collect {
          case Categories(orphas) if orphas.nonEmpty => orphas
        }
      )
    )
  
  
  // For implicit conversion of Filter DTO to predicate function
  import queryService.filterToPredicate

  def diagnostics(id: Query.Id): Action[AnyContent] =
    AuthorizedAction(OwnershipOf(id)).async { 
      implicit req =>

        queryService.resultSet(id)
          .map(_.map(_.diagnostics(FilterFrom(req))))
          .map(JsonResult(_,s"Invalid Query ID ${id.value}"))
    }

}
