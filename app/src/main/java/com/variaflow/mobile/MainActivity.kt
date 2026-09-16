package com.variaflow.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.variaflow.mobile.ui.screens.HomeScreen
import com.variaflow.mobile.ui.theme.VariaFlowTheme
import com.variaflow.mobile.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VariaFlowTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
