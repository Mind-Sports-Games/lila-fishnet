package lila.fishnet

import play.api.libs.json._

import strategygames.format.{ FEN, LexicalUci, Uci, UciDump }
import strategygames.variant.Variant
import lila.fishnet.{ Work => W }

object JsonApi {

  sealed trait Request {
    val fishnet: Request.Fishnet
    def clientKey = fishnet.apikey
  }

  object Request {

    sealed trait Result

    case class Fishnet(apikey: ClientKey)

    case class Acquire(fishnet: Fishnet) extends Request

    case class PostMove(fishnet: Fishnet, move: MoveResult) extends Request with Result

    case class MoveResult(bestmove: String) {
      def uci: Option[LexicalUci] = LexicalUci(bestmove)
    }
  }

  case class Game(
      game_id: String,
      position: FEN,
      variant: Variant,
      moves: String
  )

  def fromGame(g: W.Game) =
    Game(
      game_id = g.id,
      position = g.initialFen | g.variant.initialFen,
      variant = g.variant,
      moves = g.moves
    )

  sealed trait Work {
    val id: String
    val game: Game
  }
  case class Move(
      id: String,
      level: Int,
      game: Game,
      clock: Option[Work.Clock]
  ) extends Work

  def moveFromWork(m: Work.Move) = Move(m.id.value, m.level, fromGame(m.game), m.clock)

  object readers {
    implicit val ClientKeyReads  = Reads.of[String].map(new ClientKey(_))
    implicit val VariantRead     = Reads.of[String].map(Variant.orDefault)
    implicit val FishnetReads    = Json.reads[Request.Fishnet]
    implicit val AcquireReads    = Json.reads[Request.Acquire]
    implicit val MoveResultReads = Json.reads[Request.MoveResult]
    implicit val PostMoveReads   = Json.reads[Request.PostMove]
  }

  // "position" -> FEN.fishnetFen(g.variant)(g.position),
  object writers {
    implicit val VariantWrites = Writes[Variant] { v => JsString(v.fishnetKey) }
    implicit val FENWrites     = Writes[FEN] { fen => JsString(fen.toString) }
    implicit val GameWrites: Writes[Game] = Writes[Game] { g =>
      Json.obj(
        "game_id"  -> g.game_id,
        "position" -> FEN.fishnetFen(g.variant)(g.position),
        "variant"  -> g.variant,
        "moves"    -> g.moves
      )
    }
    implicit val ClockWrites: Writes[Work.Clock] = Json.writes[Work.Clock]
    implicit val WorkIdWrites                    = Writes[Work.Id] { id => JsString(id.value) }
    implicit val WorkWrites = OWrites[Work] { work =>
      (work match {
        case m: Move =>
          Json.obj(
            "work" -> Json.obj(
              "type"  -> "move",
              "id"    -> m.id,
              "level" -> m.level,
              "clock" -> m.clock
            )
          )
      }) ++ Json.toJson(work.game).as[JsObject]
    }
  }
}
