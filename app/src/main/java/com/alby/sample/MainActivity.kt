package com.alby.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.alby.widget.AlbyWidgetScreen
import com.alby.sample.ui.theme.AlbyWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlbyWidgetTheme {
                Scaffold(containerColor = Color.LightGray, modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlbyWidgetScreen(
                        brandId = "7a54fcd4-db3a-4a32-9b44-17f047fbc41d",
                        productId = "A140344"
                    ) {
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = name, modifier = modifier)
}