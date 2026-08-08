package cu.christianrvdv.sumador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cu.christianrvdv.sumador.ui.settings.SettingsViewModel
import cu.christianrvdv.sumador.ui.settings.ThemeOption
import cu.christianrvdv.sumador.ui.sumador.SumadorScreen
import cu.christianrvdv.sumador.ui.theme.SumadorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(context)
            )
            val settingsState by settingsViewModel.state.collectAsState()

            val useDarkTheme = when (settingsState.theme) {
                ThemeOption.DARK -> true
                ThemeOption.LIGHT -> false
                ThemeOption.SYSTEM -> null
            }

            SumadorTheme(
                darkTheme = useDarkTheme,
                dynamicColor = settingsState.theme == ThemeOption.SYSTEM
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SumadorScreen(
                        modifier = Modifier.padding(innerPadding),
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}