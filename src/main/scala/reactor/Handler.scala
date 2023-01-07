package reactor

import java.io.IOException
import java.nio.channels.{Selector, SelectionKey, SocketChannel}
import java.nio.ByteBuffer
import java.nio.charset.Charset

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Handler {
  val readBufSize: Int = 1024;
  val writeBufSize: Int = 1024;
}

class Handler(sel: Selector, channel: SocketChannel)(implicit ec: ExecutionContext) extends SelKeyAttm {
  import Handler._

  var selKey: SelectionKey = null
  val readBuf = ByteBuffer.allocate(readBufSize)
  var writeBuf = ByteBuffer.allocate(writeBufSize)

  channel.configureBlocking(false)

  selKey = channel.register(sel, SelectionKey.OP_READ)
  selKey.attach(this)
  sel.wakeup()

  def run(): Try[Unit] = Try {
    if (selKey.isReadable())
      read()
    else if (selKey.isWritable())
      write()
  }
    .recover {
      case e: IOException => println(s"Handler run(): $e")
    }

  def process(): Unit = synchronized {
    var bytes = Array.empty[Byte]

    readBuf.flip()
    bytes = Array.ofDim[Byte](readBuf.remaining())
    readBuf.get(bytes, 0, bytes.length)
    print("Handler process(): " + new String(bytes, Charset.forName("ISO-8859-1")))

    writeBuf = ByteBuffer.wrap(bytes)

    selKey.interestOps(SelectionKey.OP_WRITE)
    selKey.selector().wakeup()
  }

  def read(): Unit = synchronized {
    var numBytes: Int = 0

    Try {
      numBytes = channel.read(readBuf)
      println("Handler read(): #bytes read into 'readBuf' buffer = " + numBytes)

      if (numBytes == -1) {
        selKey.cancel()
        channel.close()
        println("Handler read(): client connection might have been dropped!")
      }
      else {
        Future {
          process()
        }
          .recover {
            case e: IOException => println(s"Handler process(): $e")
          }
      }
    }
      .recover {
        case e: IOException => println(s"Handler read(): $e")
      }
  }

  def write(): Unit = {
    var numBytes: Int = 0

    Try {
      numBytes = channel.write(writeBuf)
      println("Handler write(): #bytes read from 'writeBuf' buffer = " + numBytes)

      if (numBytes > 0) {
        readBuf.clear()
        writeBuf.clear()

        selKey.interestOps(SelectionKey.OP_READ)
        selKey.selector().wakeup()
      }
    }
      .recover {
        case e: IOException => println(s"Handler write(): $e")
      }
  }
}
