// SumadorApplication.kt
package cu.christianrvdv.sumador

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Aplicación base para Hilt.
 * Debe declararse en el AndroidManifest.xml con android:name=".SumadorApplication"
 */
@HiltAndroidApp
class SumadorApplication : Application()