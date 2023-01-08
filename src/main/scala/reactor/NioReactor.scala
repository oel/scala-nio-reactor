package reactor

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.{Selector, SelectionKey, SocketChannel, ServerSocketChannel}
import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

object NioReactor {
  val poolSize: Int = 10
  val workerPool = Executors.newFixedThreadPool(poolSize)
  implicit val ec = ExecutionContext.fromExecutorService(workerPool)

  def apply(port: Int = 9090): Future[Unit] = Future {
      (new NioReactor(port)).loop()
    }
    .recover {
      case e: Exception => println(s"Reactor($port): $e")
    }

  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      NioReactor()
    else {
      Try (args(0).toInt) match {
        case Success(p) => NioReactor(p)
        case Failure(e) => println(s"Reactor main(${args(0)}): $e")
      }
    }
  }
}

class NioReactor(port: Int) {
  implicit val ec: ExecutionContext = NioReactor.ec

  val selector: Selector = Selector.open()
  val serverChannel: ServerSocketChannel = ServerSocketChannel.open()

  serverChannel.socket().bind(new InetSocketAddress(port))
  serverChannel.configureBlocking(false)

  val sk: SelectionKey = serverChannel.register(selector, SelectionKey.OP_ACCEPT)
  sk.attach(new Acceptor())

  import java.util.{Iterator => JavaIter}

  @scala.annotation.tailrec
  final def iterateSelKeys(it: JavaIter[SelectionKey]): Unit = {
    if (it.hasNext()) {
      val sk = it.next()
      it.remove()
      val attm: SelKeyAttm = sk.attachment().asInstanceOf[SelKeyAttm]
      if (attm != null)
        attm.run()
      iterateSelKeys(it)
    }
    else ()
  }

  @scala.annotation.tailrec
  final def selectorLoop(iterFn: (JavaIter[SelectionKey]) => Unit): Nothing = {
    selector.select()
    val it = selector.selectedKeys().iterator()
    iterFn(it)
    selectorLoop(iterFn)
  }

  def loop(): Unit = Try {
      selectorLoop(iterateSelKeys)
    }
    .recover {
      case e: IOException => println(s"Reactor loop(): $e")
    }

  class Acceptor extends SelKeyAttm {
    def run(): Try[Unit] = Try {
        val channel: SocketChannel = serverChannel.accept()
        if (channel != null)
          new Handler(selector, channel)
        ()
      }
      .recover {
        case e: IOException => println(s"Acceptor: $e")
      }
  }
}
