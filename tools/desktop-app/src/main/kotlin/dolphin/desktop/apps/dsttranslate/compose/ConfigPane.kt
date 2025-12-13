package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dolphin.android.apps.dsttranslate.PoHelper
import dolphin.desktop.apps.dsttranslate.AppStrings
import dolphin.desktop.apps.dsttranslate.Ini
import java.io.File
import javax.swing.JFileChooser

data class Configs(
    val dstWorkshopDir: String = "",
    val dstAssetsDir: String = "",
    val stringMap: String = "",
    val oniWorkshopDir: String = "",
    val oniAssetsDir: String = "",
) {
    constructor(ini: Ini) : this(
        ini.dstWorkshopDir,
        ini.dstAssetsDir,
        ini.dstStringMap,
        ini.oniWorkshopDir,
        ini.oniAssetsDir,
    )
}

@Composable
fun ConfigPane(
    configs: Configs,
    onConfigChange: ((configs: Configs) -> Unit)? = null,
    mode: PoHelper.Mode = PoHelper.Mode.ONI,
    onModeChange: ((mode: PoHelper.Mode) -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    val githubRoot = if (configs.dstWorkshopDir.contains("KleiWork")) {
        configs.dstWorkshopDir.substring(0, configs.dstWorkshopDir.indexOf("KleiWork") + 8)
    } else ""

    Column {
        Text(AppStrings.config_github_root, style = MaterialTheme.typography.caption)
        FileChooserPane(
            file = githubRoot,
            onFileChange = { file ->
                println("github root = ${file.absolutePath}")
                val s = File.separator
                onConfigChange?.invoke(
                    configs.copy(
                        dstWorkshopDir = "${file.absolutePath}${s}workshop-1993780385",
                        dstAssetsDir = "${file.absolutePath}${s}dst-assets",
                        stringMap = "${file.absolutePath}${s}desktop-app${s}resources${s}common${s}strings.xml",
                        oniWorkshopDir = "${file.absolutePath}${s}workshop-2906930548",
                        oniAssetsDir = "${file.absolutePath}${s}oni-assets",
                    )
                )
            },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )

        Text(AppStrings.config_workshop_dir, style = MaterialTheme.typography.caption)
        FileChooserPane(
            file = configs.dstWorkshopDir,
            onFileChange = { file ->
                // println("workshopDir = ${file.absolutePath}")
                onConfigChange?.invoke(configs.copy(dstWorkshopDir = file.absolutePath))
            },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(modifier = Modifier.requiredHeight(4.dp))
        Text(AppStrings.config_assets_dir, style = MaterialTheme.typography.caption)
        FileChooserPane(
            file = configs.dstAssetsDir,
            onFileChange = { file ->
                // println("assetsDir = ${file.absolutePath}")
                onConfigChange?.invoke(configs.copy(dstAssetsDir = file.absolutePath))
            },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(modifier = Modifier.requiredHeight(4.dp))
        Text(AppStrings.config_oni_workshop_dir, style = MaterialTheme.typography.caption)
        FileChooserPane(
            file = configs.oniWorkshopDir,
            onFileChange = { file ->
                // println("workshopDir = ${file.absolutePath}")
                onConfigChange?.invoke(configs.copy(oniWorkshopDir = file.absolutePath))
            },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(modifier = Modifier.requiredHeight(4.dp))
        Text(AppStrings.config_oni_asset_dir, style = MaterialTheme.typography.caption)
        FileChooserPane(
            file = configs.oniAssetsDir,
            onFileChange = { file ->
                // println("assetDir = ${file.absolutePath}")
                onConfigChange?.invoke(configs.copy(oniAssetsDir = file.absolutePath))
            },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )

        Text(
            AppStrings.config_strings_xml(configs.stringMap),
            style = MaterialTheme.typography.body2,
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

        Row(modifier = Modifier.padding(8.dp)) {
            TextButton(
                onClick = { onModeChange?.invoke(PoHelper.Mode.DST) },
                enabled = mode != PoHelper.Mode.DST,
            ) {
                Text(AppStrings.config_switch_to_dst_mode)
            }
            TextButton(
                onClick = { onModeChange?.invoke(PoHelper.Mode.ONI) },
                enabled = mode != PoHelper.Mode.ONI,
            ) {
                Text(AppStrings.config_switch_to_oni_mode)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewConfigPane() {
    DstTranslatorTheme {
        ConfigPane(Configs("/home/dolphin", "/home/dolphin/assets"))
    }
}
