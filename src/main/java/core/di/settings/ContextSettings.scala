package core.di.settings

final case class ContextSettings(declaration: Iterable[BeanDeclaration], scanSettings: Seq[ScanSettings]) {
  require(declaration != null, "declaration == null")
  require(scanSettings != null, "scan settings == null")
}