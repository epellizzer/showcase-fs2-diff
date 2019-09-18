package fr.ericpellizzer.fs2.diff

import java.util.concurrent.Executors

import cats.effect.{Blocker, ContextShift, IO}
import fs2.{Pipe, Pure, Stream}
import utest._

import scala.util.Try

object StreamUtilsTest extends TestSuite {
  object CSV {
    object string {
      private val regex = """"(.*)"""".r

      def unapply(s: String): Option[String] = s match {
        case regex(s) => Some(s)
        case s => Some(s)
      }
    }

    object long {
      def unapply(s: String): Option[Long] = Try(s.toLong).toOption
    }
  }

  val tests: Tests = Tests {
    test("diff") {
      case class Person(id: Long, firstName: String, lastName: String)

      val readPerson: Pipe[Pure, String, Person] = _.map(_.split(',')).collect {
        case Array(CSV.long(id), CSV.string(fn), CSV.string(ln)) => Person(id, fn, ln)
      }

      implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
      val blocker = Stream.bracket(IO(Executors.newCachedThreadPool()))(ec => IO(ec.shutdown()))
        .map(Blocker.liftExecutorService)

      def loadPersons(file: String)(implicit bl: Blocker): Stream[IO, Person] =
        fs2.io.readInputStream(IO(getClass.getResourceAsStream(s"/$file")), 1024 * 1024, bl)
          .through(fs2.text.utf8Decode)
          .through(fs2.text.lines)
          .through(readPerson)

      // Fails with an exception
      test("with differences") {
        val result = blocker
          .flatMap { implicit bl =>
            loadPersons("before.csv").through2(loadPersons("after.csv"))(StreamUtils.diff(_.id))
          }
          .compile
          .toVector
          .unsafeRunSync()

        assert(result == Vector(
          Creation(Person(10000450147L, "ROBIN", "YVES")),
          Creation(Person(10000928852L, "PINÇON-CERDEIRA", "STEPHANIE")),
          Creation(Person(10001140572L, "BRUN", "JEAN-FRANCOIS")),
          Deletion(Person(10001471159L, "GOUYA", "HERVE")),
          Creation(Person(10001849719L, "KADOUR", "NIZAR")),
          Creation(Person(10002086915L, "ROUSSIN", "ALEXANDRA")),
          Deletion(Person(10002796471L, "REMI", "MICHEL")),
          Deletion(Person(10002798659L, "DUCASSE", "PHILIPPE")),
          Deletion(Person(10002801388L, "POUGNET", "BRUNO")),
          Deletion(Person(10002805637L, "SCAMPUCCI", "JEAN-FRANCOIS")),
        ))
      }

      // WARNING! This test makes memory blow up
      test("without differences") {
        val result = blocker
          .flatMap { implicit bl =>
            val s = loadPersons("before.csv")
            s.through2(s)(StreamUtils.diff(_.id))
          }
          .compile
          .toVector
          .unsafeRunSync()

        assert(result == Vector())
      }
    }
  }
}
