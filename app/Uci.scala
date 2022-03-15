package lila.uci

import org.playstrategy.FairyStockfish.{ availablePieceChars, availablePromotablePieceChars, init };

// We need a more generalized, simple notion of UCI here.
// Without a variant we can't fully parse UCI and ensure it's validate it
// So instead we do a simpler validation here, which is based on simple
// structure of uci.
//
trait Uci {
  val uci: String
}

object Uci {

  init()

  def apply(s: String): Option[Uci] = {
    if (validUci(s)) Some(UciImpl(s))
    else None
  }

  val availablePieces = availablePieceChars().getString()
  val availablePromotablePieces = availablePromotablePieceChars().getString()

  def validRole(c: Char): Boolean = availablePieces.exists(c.==)
  def validPromotableRole(c: Char): Boolean =
    c == '+' || availablePromotablePieces.exists(c.==)
  def validFile(c: Char): Boolean = ('a' to 'i').exists(c.==)
  def validRank(s: String): Boolean =
    (s.length() == 1 && ('0' to '9').exists(s(0).==)) || (s.length() == 2 && s == "10")
  def validSquare(s: String): Boolean =
    (s.nonEmpty && validFile(s(0))) &&
      ((s.length() == 2 && validRank(s.slice(1, 2))) ||
        (s.length() == 3 && validRank(s.slice(1, 3))))

  def validSquarePair(s: String): Boolean =
    s.length() match {
      case 4 => validSquare(s.slice(0, 2)) && validSquare(s.slice(2, 4))
      case 5 =>
        (validSquare(s.slice(0, 2)) && validSquare(s.slice(2, 5))) || (validSquare(
          s.slice(0, 3)
        ) && validSquare(s.slice(3, 5)))
      case 6 => validSquare(s.slice(0, 3)) && validSquare(s.slice(3, 6))
      case _ => false
    }

  def validUci(s: String): Boolean =
    if (s.length() < 4 || s.length() > 7) false
    else if (s == "0000") true
    else {
      val isDrop      = validRole(s(0)) && s(1) == '@'
      val isPromotion = validPromotableRole(s.last)
      (isDrop, isPromotion, s.length()) match {
        // Drops
        case (true, false, 4 | 5) => validRole(s(0)) && validSquare(s.slice(2, s.length())) // P@b4
        // Promotions
        case (false, true, 5 | 6 | 7) =>
          validSquarePair(s.slice(0, s.length() - 1)) // d8d9+ | d8d9R | d10d11+
        // moves
        case (false, false, 4 | 5 | 6) => validSquarePair(s.slice(0, s.length())) // d8d9 | d9d11 | d10d11

        // Bleh
        case _ => false
      }
    }

  private case class UciImpl(uci: String) extends Uci
}
