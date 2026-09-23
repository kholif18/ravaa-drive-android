package com.ravaa.drive.ui.theme

import com.ravaa.drive.R

/**
 * Kora icon theme (https://github.com/bikass/kora) — GPL-3.0.
 * Port dari web: components/drive/kora-icon.tsx (getKoraFileIcon).
 * PNG 192px di drawable-nodpi (nama file: kora_<nama>).
 */
object KoraIcons {
    fun forFile(name: String, mimeType: String): Int {
        val n = name.lowercase()
        val m = mimeType.lowercase()

        if (n.matches(Regex(".*\\.(png|jpe?g|gif|webp|svg|bmp|ico|heic|avif)$"))) {
            return R.drawable.kora_image_x_generic
        }
        if (n.matches(Regex(".*\\.(mp4|webm|mkv|avi|mov|wmv|flv|m4v|3gp)$"))) {
            return R.drawable.kora_video_x_generic
        }
        if (m.startsWith("audio/") || n.matches(Regex(".*\\.(mp3|wav|flac|aac|ogg|m4a|opus)$"))) {
            return R.drawable.kora_audio_x_generic
        }
        if (n.endsWith(".pdf")) return R.drawable.kora_application_pdf

        if (m.contains("zip") || n.endsWith(".zip")) return R.drawable.kora_application_zip
        if (m.contains("rar") || n.endsWith(".rar")) return R.drawable.kora_application_x_rar
        if (m.contains("7z") || n.endsWith(".7z")) return R.drawable.kora_application_7zip
        if (m.contains("archive") || n.matches(Regex(".*\\.(tar|gz|bz2|xz|tgz|zst)$"))) {
            return R.drawable.kora_application_archive
        }

        // Word: .doc / .docx / .dotx
        if (n.matches(Regex(".*\\.docx?$")) || m.contains("wordprocessingml") || m.contains("msword") || m.contains("ms-word")) {
            return R.drawable.kora_application_msword
        }
        // Excel: .xls / .xlsx / .xlsm / .csv
        if (n.matches(Regex(".*\\.xlsx?$")) || n.endsWith(".xlsm") || m.contains("spreadsheetml") || m.contains("ms-excel")) {
            return R.drawable.kora_application_vnd_ms_excel
        }
        // PowerPoint: .ppt / .pptx / .pps
        if (n.matches(Regex(".*\\.pptx?$")) || n.matches(Regex(".*\\.ppsx?$")) || m.contains("presentationml") || m.contains("ms-powerpoint")) {
            return R.drawable.kora_application_vnd_ms_powerpoint
        }
        // LibreOffice / OpenDocument
        if (m.contains("opendocument.text") || n.endsWith(".odt")) {
            return R.drawable.kora_application_vnd_oasis_opendocument_text
        }
        if (m.contains("opendocument.spreadsheet") || n.endsWith(".ods")) {
            return R.drawable.kora_application_vnd_oasis_opendocument_spreadsheet
        }
        if (m.contains("opendocument.presentation") || n.endsWith(".odp")) {
            return R.drawable.kora_application_vnd_oasis_opendocument_presentation
        }
        if (m.contains("opendocument.graphics") || n.endsWith(".odg")) {
            return R.drawable.kora_application_vnd_oasis_opendocument_graphics
        }
        if (m.contains("rtf") || n.endsWith(".rtf")) return R.drawable.kora_text_rtf
        if (m.contains("csv") || n.endsWith(".csv")) return R.drawable.kora_text_csv
        if (m.contains("markdown") || n.matches(Regex(".*\\.mdx?$"))) return R.drawable.kora_text_markdown
        if (m.contains("html") || n.matches(Regex(".*\\.html?$"))) return R.drawable.kora_text_html
        if (m.contains("xml") || n.endsWith(".xml")) return R.drawable.kora_application_xml

        if (m.contains("json") || n.endsWith(".json")) return R.drawable.kora_application_json
        if (m.contains("javascript") || n.matches(Regex(".*\\.m?jsx?$")) || n.matches(Regex(".*\\.tsx?$"))) {
            return R.drawable.kora_application_javascript
        }
        if (m.contains("python") || n.matches(Regex(".*\\.pyw?$"))) return R.drawable.kora_text_x_python
        if (m.contains("java") || n.endsWith(".java")) return R.drawable.kora_text_x_java
        if (m.contains("c++") || n.matches(Regex(".*\\.(cpp|cxx|cc|hpp|hxx)$"))) return R.drawable.kora_text_x_cpp
        if (m.contains("c-source") || n.endsWith(".c") || n.endsWith(".h")) return R.drawable.kora_text_x_c
        if (m.contains("php") || n.endsWith(".php")) return R.drawable.kora_application_x_php
        if (m.contains("shellscript") || n.matches(Regex(".*\\.(sh|bash|zsh)$"))) {
            return R.drawable.kora_application_x_shellscript
        }
        if (m.contains("sql") || n.endsWith(".sql")) return R.drawable.kora_application_sql
        if (m.contains("tex") || n.matches(Regex(".*\\.(tex|ltx)$"))) return R.drawable.kora_text_x_tex
        if (m.contains("msdownload") || n.matches(Regex(".*\\.(exe|msi|dll)$"))) {
            return R.drawable.kora_application_x_msdownload
        }
        return R.drawable.kora_text_x_generic
    }

    /** File gambar/video: coba thumbnail server dulu, fallback icon Kora. */
    fun isPreviewable(name: String, mimeType: String): Boolean {
        val n = name.lowercase()
        val m = mimeType.lowercase()
        return m.startsWith("image/") || m.startsWith("video/") ||
            n.matches(Regex(".*\\.(png|jpe?g|gif|webp|bmp|heic|avif|mp4|webm|mkv|avi|mov)$"))
    }
}
