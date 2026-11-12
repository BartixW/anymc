/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ZipUtils {
    fun unzip(zipFile: File, destDir: File) {
        if (!destDir.exists()) destDir.mkdirs()
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val entryFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(entryFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    fun readEntry(zipFile: File, entryName: String): String? {
        return try {
            ZipFile(zipFile).use { zip ->
                zip.getEntry(entryName)?.let { entry ->
                    zip.getInputStream(entry).use { input ->
                        input.bufferedReader().readText()
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun zip(sourceDir: File, zipFile: File, filter: (File) -> Boolean = { true }) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zipFolder(sourceDir, sourceDir, zos, filter)
        }
    }

    private fun zipFolder(root: File, source: File, zos: ZipOutputStream, filter: (File) -> Boolean) {
        source.listFiles()?.forEach { file ->
            if (!filter(file)) return@forEach

            if (file.isDirectory) {
                zipFolder(root, file, zos, filter)
            } else {
                val entryName = file.absolutePath.substring(root.absolutePath.length + 1)
                val entry = ZipEntry(entryName)
                zos.putNextEntry(entry)
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}
