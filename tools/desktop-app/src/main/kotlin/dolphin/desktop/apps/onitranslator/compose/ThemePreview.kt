package dolphin.desktop.apps.onitranslator.compose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ThemePreview(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text("M3 Light Theme", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { /*TODO*/ }) {
                Text("Primary Button")
            }
            Spacer(Modifier.height(8.dp))
            Text("This is a sample text.", color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.primaryContainer))
        }
    }
}

@Preview
@Composable
fun PreviewThemes() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Light Theme Preview
        OniTranslatorM3Theme(darkTheme = false) {
            ThemePreview(modifier = Modifier.weight(1f))
        }

        // Dark Theme Preview
        OniTranslatorM3Theme(darkTheme = true) {
            ThemePreview(modifier = Modifier.weight(1f))
        }
    }
}
