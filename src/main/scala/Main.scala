import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import upickle.default._
import org.scalajs.dom

sealed trait Page
case object HomePage extends Page
case object AuthPage extends Page
case object APage extends Page

object Router {
  private implicit val pageRW: ReadWriter[Page] = macroRW[Page]

  private val routes = List(
    Route.static(HomePage, root / endOfSegments),
    Route.static(AuthPage, root / "login" / endOfSegments),
    Route.static(APage, root / "apage" / endOfSegments)
  )

  private val router = new Router[Page](
    initialUrl = dom.document.location.href,
    origin = dom.document.location.origin.get,
    routes = routes,
    owner = unsafeWindowOwner,
    $popStateEvent = windowEvents.onPopState,
    getPageTitle = _.toString,
    serializePage = page => write(page)(pageRW),
    deserializePage = pageStr => read(pageStr)(pageRW)
  )
  val currentPageSignal = router.$currentPage
  val pushState = Observer[Page] { (page: Page) =>
    if (router.$currentPage.now() != page) {
      router.pushState(page)
    }
  }
}

case class User(id: String)

object Authentication {
  private val userVar: Var[Option[User]] = Var(None)
  val userSignal = userVar.signal
  
  def render(redirect: Option[User => HtmlElement]) = div(
    child <-- Authentication.userSignal.map {
    case None =>
      button("Click to login", onClick.mapTo(Some(User("id"))) --> Authentication.userVar )
    case Some(user) =>
      redirect match {
        case Some(f) => f(user) // Coming from another page
        case None => // Login page requested using its route
          div(
            p("You're logged in"),
            button("Continue", onClick.mapToValue(HomePage) --> Router.pushState),
            button("Logout", onClick.mapToValue(None) --> Authentication.userVar)
          )
      }
    }
  )
}

object Main {

  def renderAPage(user: User): HtmlElement = {
    div("A Page needing authentication. You are ", user.id)
  }

  def withAuthUser(f: User => HtmlElement): HtmlElement = {
    Authentication.userSignal.now() match {
      case Some(user) => f(user)
      case None       => Authentication.render(Some(f))
    }
  }

  val splitter = SplitRender[Page, HtmlElement](Router.currentPageSignal)
    .collectStatic(HomePage)(div(
      button("Login", onClick.mapToValue(AuthPage) --> Router.pushState),
      button("A page", onClick.mapToValue(APage) --> Router.pushState),
    ))
    .collectStatic(AuthPage)(Authentication.render(None))
    .collectStatic(APage)(withAuthUser(renderAPage))

  val app = div(
    child <-- splitter.$view
  )

  def main(args: Array[String]): Unit = {
    render(dom.document.getElementById("app"), app)
  }
}