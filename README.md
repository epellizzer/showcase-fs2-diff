# FS2 diff

This project contains small pieces of code showcasing the computation of a diff between
two FS2 streams.

The interesting file is [StreamUtils.scala](/src/main/scala/fr/ericpellizzer/fs2/diff/StreamUtils.scala).

It uses SBT, FS2 and µTest.


# Current issues

The implementation suffers from two issues.
 
First, it crashes at the end of the stream with the following error :

```plain
Fail to find scope for next step: current: Token(61eefabb), step: Step(FreeC.Bind(FreeC.Bind(FreeC.Pure(()))),Some(Token(8798dd1)))
java.lang.Throwable: Fail to find scope for next step: current: Token(61eefabb), step: Step(FreeC.Bind(FreeC.Bind(FreeC.Pure(()))),Some(Token(8798dd1)))
	at fs2.internal.Algebra$.$anonfun$compileLoop$5(Algebra.scala:294)
	at cats.effect.internals.IORunLoop$.cats$effect$internals$IORunLoop$$loop(IORunLoop.scala:139)
	at cats.effect.internals.IORunLoop$RestartCallback.signal(IORunLoop.scala:355)
	at cats.effect.internals.IORunLoop$RestartCallback.apply(IORunLoop.scala:376)
	at cats.effect.internals.IORunLoop$RestartCallback.apply(IORunLoop.scala:316)
	at cats.effect.internals.IORunLoop$.cats$effect$internals$IORunLoop$$loop(IORunLoop.scala:136)
	at cats.effect.internals.IORunLoop$RestartCallback.signal(IORunLoop.scala:355)
	at cats.effect.internals.IORunLoop$RestartCallback.apply(IORunLoop.scala:376)
	at cats.effect.internals.IORunLoop$RestartCallback.apply(IORunLoop.scala:316)
	at cats.effect.internals.IOShift$Tick.run(IOShift.scala:36)
	at java.base/java.util.concurrent.ForkJoinTask$RunnableExecuteAction.exec(ForkJoinTask.java:1425)
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:177)
```

Second, in the case when there are no differences between the two streams, the heap blows up.


# Diagnostics

Two JFR recordings are available in the [`profiling`](/profiling) folder, one for each test case. They can be opened
with Java Mission Control.

The one concerning the case that hangs forever has been recorded by letting the JVM go wild for around 15 seconds and 
then sending it a TERM signal. Another 10 seconds were necessary for it to acknowledge and finally terminate. Killing
the JVM would leave a truncated or empty recording.
