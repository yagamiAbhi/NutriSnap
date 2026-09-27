package com.nutrisnap.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("TODAY", style = MaterialTheme.typography.headlineMedium)
        Text("Calories: 1842 / 2100 kcal")
        Text("Protein: 118 / 130 g")
        Text("Carbohydrates: 238 / 280 g")
        Text("Fat: 54 / 70 g")
        Text("Fiber: 31 / 35 g")

        Button(onClick = {}) {
            Text("Add food")
        }
    }
}
