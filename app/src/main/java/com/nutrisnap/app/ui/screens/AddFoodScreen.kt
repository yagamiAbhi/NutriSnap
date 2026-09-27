package com.nutrisnap.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AddFoodScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = {}) {
            Text("Analyze with Camera")
        }
        Button(onClick = {}) {
            Text("Enter Manually")
        }
        Button(onClick = {}) {
            Text("Saved Foods")
        }
    }
}
