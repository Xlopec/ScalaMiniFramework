package core.di.settings

final case class ScanSettings(pack: String) {
  require(pack != null && !pack.isEmpty, s"empty package settings, was $pack")
}
