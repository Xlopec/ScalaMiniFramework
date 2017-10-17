package core.di.imp

import core.di.settings.Property

final class Context(props: Map[String, Property]) {
  val properties: Map[String, Property] = props
}
