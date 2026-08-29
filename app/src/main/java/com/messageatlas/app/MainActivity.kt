package com.messageatlas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.messageatlas.app.ui.MessageAtlasRoot
import com.messageatlas.app.ui.MessageAtlasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MessageAtlasTheme { MessageAtlasRoot(viewModel()) } }
    }
}

