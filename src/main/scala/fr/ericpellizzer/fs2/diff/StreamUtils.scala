package fr.ericpellizzer.fs2.diff

import fs2.{Pipe, Pipe2, Pure, Stream}

import scala.language.higherKinds

object StreamUtils {
  /** Computes a running diff between two streams.
   *
   * The resulting stream emits the changes that should be applied to the left stream to obtain the right stream.
   *
   * In order to determine the changes, a discriminating value is needed for each emitted element. The two piped
   * streams must already be sorted in ascending order of that value.
   *
   * Since the implementation needs to put the corresponding elements from both stream "side-by-side", it has to
   * generate all the necessary intermediate discriminating values. For this reason, the values associated to the
   * emitted elements must form a reasonably compact space, to avoid generating millions of intermediate values.
   *
   * @param f a function to obtain the discriminating value for each emitted element.
   * @tparam F a type of effet.
   * @tparam A the type of emitted values.
   * @return a [[Pipe2]] that produces a stream emitting [[Change]]s.
   */
  def diff[F[_], A](f: A => Long): Pipe2[F, A, A, Change[A]] = { (left, right) =>
    // Generate intermediate values and map them to 'None'
    val expand: Pipe[F, A, Option[A]] =
      _.zipWithNext
        .flatMap {
          case (a, Some(na)) => Stream(Some(a)) ++ range(f(a) + 1L, f(na)).as(None)
          case (a, None) => Stream(Some(a))
        }

    // Expand both streams, zip them together and compute the differences
    def relate: Pipe2[F, A, A, Change[A]] = (l, r) =>
      l.through(expand)
        .zipAll(r.through(expand))(None, None)
        .collect {
          case (Some(al), Some(ar)) if al != ar => Modification(al, ar)
          case (Some(al), None) => Deletion(al)
          case (None, Some(ar)) => Creation(ar)
        }

    // Pick the right starting point and relate the streams
    left.pull.peek1.flatMap {
      case None => right.pull.echo.mapOutput(Creation.apply) // left empty -> only creations
      case Some((hl, tl)) => right.pull.peek1.flatMap {
        case None => tl.pull.echo.mapOutput(Deletion.apply) // right empty -> only deletions
        case Some((hr, tr)) =>
          val nhl = f(hl)
          val nhr = f(hr)
          if (nhl > nhr) // which streams starts at the lowest value?
            // the right one -> only creations up to that point, then relate the rest
            tr.pull.takeWhile(f(_) < nhl).mapOutput(Creation.apply).flatMap {
              case None => tl.pull.echo.mapOutput(Deletion.apply)
              case Some(s) => tl.through2(s)(relate).pull.echo
            }
          else if (nhl < nhr)
            // the left one -> only deletions up to that point, then relate the rest
            tl.pull.takeWhile(f(_) < nhr).mapOutput(Deletion.apply).flatMap {
              case None => tr.pull.echo.mapOutput(Creation.apply)
              case Some(s) => s.through2(tr)(relate).pull.echo
            }
          else
            // same starting value, just relate the original streams
            tl.through2(tr)(relate).pull.echo
      }
    }.stream
  }

  // A copy of 'Stream.range' for 'Long' values
  def range(start: Long, stopExclusive: Long, by: Long = 1L): Stream[Pure, Long] =
    Stream.unfold(start) { i =>
      if ((by > 0 && i < stopExclusive && start < stopExclusive) ||
        (by < 0 && i > stopExclusive && start > stopExclusive))
        Some((i, i + by))
      else None
    }
}
