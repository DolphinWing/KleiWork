package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.dsttranslate.Ini
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.github_root
import dolphin.desktop.apps.onitranslator.generated.resources.oni_asset_dir
import dolphin.desktop.apps.onitranslator.generated.resources.oni_workshop_dir
import dolphin.desktop.apps.onitranslator.generated.resources.strings_xml
import org.jetbrains.compose.resources.stringResource
import java.io.File
import javax.swing.JFileChooser

data class Configs(
    val stringMap: String = "",
    val oniWorkshopDir: String = "",
    val oniAssetsDir: String = "",
) {
    constructor(ini: Ini) : this(
        ini.stringMap,
        ini.oniWorkshopDir,
        ini.oniAssetsDir,
    )
}

@Composable
fun ConfigPane(
    configs: Configs,
    onConfigChange: ((configs: Configs) -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    val githubRoot = ""

    Column {
        Text(stringResource(Res.string.github_root), style = MaterialTheme.typography.caption)
        FileChooserPane(
            file = githubRoot,
            onFileChange = { file ->
                println("github root = ${file.absolutePath}")
                val s = File.separator
                onConfigChange?.invoke(
                    configs.copy(
                        stringMap = "${file.absolutePath}${s}desktop-app${s}resources${s}common${s}strings.xml",
                        oniWorkshopDir = "${file.absolutePath}${s}workshop-2906930548",
                        oniAssetsDir = "${file.absolutePath}${s}oni-assets",
                    )
                )
            },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )

        Text(stringResource(Res.string.oni_workshop_dir), style = MaterialTheme.typography.caption)
        FileChooserPane(
            file = configs.oniWorkshopDir,
            onFileChange = { file ->
                // println("workshopDir = ${file.absolutePath}")
                onConfigChange?.invoke(configs.copy(oniWorkshopDir = file.absolutePath))
            },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(modifier = Modifier.requiredHeight(4.dp))
        Text(stringResource(Res.string.oni_asset_dir), style = MaterialTheme.typography.caption)
        FileChooserPane(
            file = configs.oniAssetsDir,
            onFileChange = { file ->
                // println("assetDir = ${file.absolutePath}")
                onConfigChange?.invoke(configs.copy(oniAssetsDir = file.absolutePath))
            },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )

        Text(
            stringResource(Res.string.strings_xml, configs.stringMap), style = MaterialTheme.typography.body2,
            modifier = Modifier.clickable { visible = true }.padding(8.dp),
            color = if (configs.stringMap.isEmpty()) Color.Red else MaterialTheme.typography.caption.color,
        )
        if (visible) {
            FileChooserPane(file = configs.stringMap, onFileChange = { file ->
                // println("strings.xml = ${file.absolutePath}")
                onConfigChange?.invoke(configs.copy(stringMap = file.absolutePath))
            })
            Spacer(modifier = Modifier.requiredHeight(4.dp))
        }
    }
}

@Preview
@Composable
private fun PreviewConfigPane() {
    OniTranslatorTheme {
        ConfigPane(Configs(oniWorkshopDir = "workshop-2906930548", oniAssetsDir = "assets")) {}
    }
}
