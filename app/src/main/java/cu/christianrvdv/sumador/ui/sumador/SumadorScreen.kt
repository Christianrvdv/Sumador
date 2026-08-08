// SumadorScreen.kt
package cu.christianrvdv.sumador.ui.sumador

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SumadorScreen(
    modifier: Modifier = Modifier,
    viewModel: SumadorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    // Animación para el total (pequeño efecto de escala)
    val totalScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "total_scale"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título con icono
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Money,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Sumador de Efectivo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Lista de campos por denominación
            denominaciones.forEach { denom ->
                BillInputRow(
                    denomination = denom,
                    value = state.cantidades[denom] ?: "",
                    onValueChange = { newValue ->
                        viewModel.updateCantidad(denom, newValue)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Botón calcular (opcional, pero mantiene la interacción manual)
            FilledTonalButton(
                onClick = { viewModel.calcularTotal() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = "Calcular")
                Spacer(Modifier.width(10.dp))
                Text("Calcular total", style = MaterialTheme.typography.titleLarge)
            }

            // Tarjeta del resultado con animación
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(totalScale),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${state.total} $",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun BillInputRow(
    denomination: Int,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del billete
            Icon(
                imageVector = Icons.Default.Money,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))

            Text(
                text = "Billetes de $denomination$",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(100.dp),
                placeholder = { Text("0") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
        }
    }
}