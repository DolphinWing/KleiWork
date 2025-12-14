package dolphin.desktop.apps.dsttranslate

import java.util.*

// TODO: Need to determine the base name for the resource bundle.
// Assuming "strings" for now, and locale defaults to current system.
object AppStrings {
    private val resourceBundle: ResourceBundle by lazy {
        try {
            ResourceBundle.getBundle("strings", Locale.getDefault())
        } catch (e: MissingResourceException) {
            ResourceBundle.getBundle("strings", Locale.ROOT) // Fallback to default
        }
    }

    // General
    val app_name: String
        get() = resourceBundle.getString("app_name")
    val content_no_items: String
        get() = resourceBundle.getString("content_no_items")

    // Tabs
    val tab_config: String
        get() = resourceBundle.getString("tab_config")
    val tab_translation: String
        get() = resourceBundle.getString("tab_translation")

    // Buttons
    val button_apply: String
        get() = resourceBundle.getString("button_apply")
    val button_cancel: String
        get() = resourceBundle.getString("button_cancel")
    val button_close: String
        get() = resourceBundle.getString("button_close")
    val button_edit: String
        get() = resourceBundle.getString("button_edit")
    val button_refresh: String
        get() = resourceBundle.getString("button_refresh")
    val button_save: String
        get() = resourceBundle.getString("button_save")
    val button_no: String
        get() = resourceBundle.getString("button_no")
    val button_yes: String
        get() = resourceBundle.getString("button_yes")
    val button_search: String
        get() = resourceBundle.getString("button_search")

    // Content Descriptions
    val content_description_cached: String
        get() = resourceBundle.getString("content_description_cached")
    val content_description_undo: String
        get() = resourceBundle.getString("content_description_undo")
    val content_description_clear: String
        get() = resourceBundle.getString("content_description_clear")
    val content_description_folder: String
        get() = resourceBundle.getString("content_description_folder")

    // Search
    val search_placeholder: String
        get() = resourceBundle.getString("search_placeholder")
    val search_type_key: String
        get() = resourceBundle.getString("search_type_key")
    val search_type_origin: String
        get() = resourceBundle.getString("search_type_origin")
    val search_type_text: String
        get() = resourceBundle.getString("search_type_text")

    // Tooltips
    val tooltip_source_text: String
        get() = resourceBundle.getString("tooltip_source_text")
    val tooltip_now_text: String
        get() = resourceBundle.getString("tooltip_now_text")
    val tooltip_simplified_chinese_text: String
        get() = resourceBundle.getString("tooltip_simplified_chinese_text")
    val tooltip_use_this_text: String
        get() = resourceBundle.getString("tooltip_use_this_text")
    val tooltip_copy_original_text: String
        get() = resourceBundle.getString("tooltip_copy_original_text")
    val tooltip_send_to_google_translate: String
        get() = resourceBundle.getString("tooltip_send_to_google_translate")
    val tooltip_show_link: String
        get() = resourceBundle.getString("tooltip_show_link")
    val tooltip_copy_this_text: String
        get() = resourceBundle.getString("tooltip_copy_this_text")
    val tooltip_copy_all: String
        get() = resourceBundle.getString("tooltip_copy_all")
    val tooltip_paste_all: String
        get() = resourceBundle.getString("tooltip_paste_all")

    // Configs
    val github_root: String
        get() = resourceBundle.getString("github_root")
    val workshop_dir: String
        get() = resourceBundle.getString("workshop_dir")
    val assets_dir: String
        get() = resourceBundle.getString("assets_dir")
    val oni_workshop_dir: String
        get() = resourceBundle.getString("oni_workshop_dir")
    val oni_asset_dir: String
        get() = resourceBundle.getString("oni_asset_dir")

    // Toolbar
    val toolbar_simplified_text: String
        get() = resourceBundle.getString("toolbar_simplified_text")
    val toolbar_template_text: String
        get() = resourceBundle.getString("toolbar_template_text")
    val toolbar_old_translated: String
        get() = resourceBundle.getString("toolbar_old_translated")
    val toolbar_now_translated: String
        get() = resourceBundle.getString("toolbar_now_translated")

    // Toasts
    val toast_write_failed: String
        get() = resourceBundle.getString("toast_write_failed")

    // Formatted Strings
    fun strings_xml(param: String): String =
        String.format(resourceBundle.getString("strings_xml"), param)
    fun toolbar_status(allCount: Int, changedCount: Int): String =
        String.format(resourceBundle.getString("toolbar_status"), allCount, changedCount)
    fun debug_save_dialog_title(param: String): String =
        String.format(resourceBundle.getString("debug_save_dialog_title"), param)
    fun toast_write_success(exported: String, cost: Long): String =
        String.format(resourceBundle.getString("toast_write_success"), exported, cost)
    fun toast_cost_ms(cost: Long): String =
        String.format(resourceBundle.getString("toast_cost_ms"), cost)

    // TODO: Add a way to change locale dynamically if needed
    // fun setLocale(locale: Locale) {
    //     this.locale = locale
    //     // Invalidate resourceBundle lazy delegate to reload with new locale
    //     // This might require a more sophisticated implementation if used frequently.
    // }
}
