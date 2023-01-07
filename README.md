# Scala NIO-based Reactor

This is a barebone server application written in Scala using [Java NIO API](https://docs.oracle.com/javase/8/docs/api/java/nio/package-summary.html) to implement the [Reactor pattern](https://en.wikipedia.org/wiki/Reactor_pattern) with non-blocking I/O operations.

For an overview of the application including how NIO fits into implementing the Reactor pattern, please read this [blog post](https://blog.genuine.com/2023/01/nio-based-reactor-in-scala/).  The application is a rewrite (in Scala) of a Java application described in an old [blog post](https://blog.genuine.com/2013/07/nio-based-reactor/).

### To build and start up the Reactor server

Git-clone the repo to a host designated for the server, open up a shell command line terminal and use a build tool such as *sbt* to compile and run the server from the *project-root*.

```bash
$ sbt compile
$ sbt "runMain reactor.NioReactor [<port>]"
```

Skipping the optional \<port\> argument will bind the server to the default port *9090*.

### To verify whether the Reactor server is up

Use [lsof](https://man7.org/linux/man-pages/man8/lsof.8.html) to verify whether the server has been started up (if so, the corresponding process ID will be returned).

```bash
$ lsof -ti tcp:<port>
# e.g. lsof -ti tcp:9090
```

### To connect to the Reactor server

Use [telnet](https://www.digitalocean.com/community/tutorials/telnet-command-linux-unix) from one or more client host terminals and enter random text followed by carriage returns.

```bash
$ telnet <server-host> <port>
# e.g. telnet reactor.example.com 8080, telnet localhost 9090
```

Observe at the client host(s) the input text being echoed back by the Reactor server, which itself also reports byte-level statistics.
