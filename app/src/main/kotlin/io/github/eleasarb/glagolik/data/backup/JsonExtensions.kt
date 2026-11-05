package io.github.eleasarb.glagolik.data.backup

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

internal fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)

internal fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)

internal fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

internal fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (isNull(key)) null else getDouble(key)
