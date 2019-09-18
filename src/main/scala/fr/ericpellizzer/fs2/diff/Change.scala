package fr.ericpellizzer.fs2.diff

sealed trait Change[A]

case class Creation[A](value: A) extends Change[A]
case class Modification[A](oldValue: A, newValue: A) extends Change[A]
case class Deletion[A](oldValue: A) extends Change[A]
