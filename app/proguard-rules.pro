# Mantener todas las clases anotadas con @Keep
-keep @androidx.annotation.Keep class *

# Mantener clases específicas de backup (opcional, ya que @Keep las cubre)
-keep class cu.christianrvdv.sumador.data.BackupData { *; }
-keep class cu.christianrvdv.sumador.ui.settings.SettingsState { *; }
-keep class cu.christianrvdv.sumador.ui.settings.ThemeOption { *; }
-keep class cu.christianrvdv.sumador.ui.settings.CurrencySymbol { *; }
-keep class cu.christianrvdv.sumador.ui.settings.LanguageOption { *; }
-keep class cu.christianrvdv.sumador.data.database.SavedSumEntity { *; }
-keep class cu.christianrvdv.sumador.data.database.CustomDenominationEntity { *; }

# Mantener constructores de data classes para Gson
-keepclassmembers class cu.christianrvdv.sumador.data.** {
    <init>(...);
}
-keepclassmembers class cu.christianrvdv.sumador.ui.settings.** {
    <init>(...);
}