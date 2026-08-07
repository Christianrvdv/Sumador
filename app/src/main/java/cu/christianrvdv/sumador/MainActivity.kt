package cu.christianrvdv.sumador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.christianrvdv.sumador.ui.theme.SumadorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SumadorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SumadorScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Datos de las denominaciones
val denominaciones = listOf(10, 20, 50, 100, 1000, 2000, 5000)

@Composable
fun SumadorScreen(modifier: Modifier = Modifier) {
    // Estado para almacenar la cantidad de cada denominación (como String para el TextField)
    val cantidades = remember { mutableStateMapOf<Int, String>().apply {
        denominaciones.forEach { this[it] = "" }
    } }

    var total by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sumador de dinero (CUP)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Campos para cada denominación
        denominaciones.forEach { denom ->
            BillInputRow(
                denomination = denom,
                value = cantidades[denom] ?: "",
                onValueChange = { newValue ->
                    cantidades[denom] = newValue
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                total = denominaciones.sumOf { denom ->
                    val quantity = cantidades[denom]?.toIntOrNull() ?: 0
                    denom * quantity
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Calcular total")
        }

        // Mostrar resultado
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = "Total: $total $",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun BillInputRow(
    denomination: Int,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Billetes de ${denomination}$:",
            modifier = Modifier.weight(1f),
            fontSize = 16.sp
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.width(100.dp),
            placeholder = { Text("0") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SumadorScreenPreview() {
    SumadorTheme {
        SumadorScreen()
    }
}