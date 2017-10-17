package core.di.settings

final case class ContextSettings(declaration: Iterable[BeanDeclaration]) {
  require(declaration != null, "declaration == null")
}