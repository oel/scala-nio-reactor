package reactor

import scala.util.Try

trait SelKeyAttm {
  def run(): Try[Unit]
}
