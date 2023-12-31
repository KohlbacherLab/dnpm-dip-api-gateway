package de.dnpm.dip.rest.api


import java.net.URI
import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._
import de.dnpm.dip.rest.util.Extractor


//object Uri extends Extractor(URI.create)


class CatalogRouter @Inject()(
  catalogController: CatalogController
)
extends SimpleRouter
{

  val Uri =
    Extractor(URI.create)


  override val routes: Routes = {

    case GET(p"/coding/codesystems"
              ?q"uri=${Uri(uri)}"
              ?q_o"version=$version")  => catalogController.codeSystem(uri,version) 

    case GET(p"/coding/codesystems")   => catalogController.codeSystemInfos


    case GET(p"/coding/valuesets"
              ?q"uri=${Uri(uri)}"
              ?q_o"version=$version")  => catalogController.valueSet(uri,version) 

    case GET(p"/coding/valuesets")     => catalogController.codeSystemInfos

  }


}
