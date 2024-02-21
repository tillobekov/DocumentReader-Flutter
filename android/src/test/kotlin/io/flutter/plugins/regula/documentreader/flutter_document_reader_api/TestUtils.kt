//
//  Convert.java
//  DocumentReader
//
//  Created by Pavel Masiuk on 21.09.2023.
//  Copyright © 2023 Regula. All rights reserved.
//
package io.flutter.plugins.regula.documentreader.flutter_document_reader_api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.robolectric.shadow.api.Shadow
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64

fun readFile(name: String): JSONObject {
    val bytes = Files.readAllBytes(Paths.get("../test/json/$name.json"))
    return JSONObject(String(bytes))
}

fun compareJSONs(name: String, expected: JSONObject, actual: JSONObject) =
    try {
        JSONAssert.assertEquals(expected, actual, false)
    } catch (e: Throwable) {
        println("\nAndroid test failed: $name")
        println(" Expected JSON:\n$expected")
        println(" Actual JSON:\n$actual")
        throw e
    }

fun <T> compareSingle(
    name: String,
    fromJson: (JSONObject) -> T,
    toJson: (T) -> JSONObject?,
    vararg omit: String
) {
    try {
        var expected = readFile(name + "Nullable")
        for (key in omit) expected = omitDeep(expected, key.split("."), 0)
        val actual = toJson(fromJson(expected))!!
        compareJSONs(name, expected, actual)
    } catch (_: IOException) {
    }
}

fun <T> compare(
    name: String,
    fromJson: (JSONObject) -> T,
    toJson: (T) -> JSONObject?,
    vararg omit: String
) {
    compareSingle(name, fromJson, toJson, *omit)
    compareSingle(name + "Nullable", fromJson, toJson, *omit)
}

fun omitDeep(dict: JSONObject, path: List<String>, index: Int): JSONObject {
    if (index < path.size - 1) {
        val node = dict.get(path[index])
        if (node is JSONObject)
            dict.put(path[index], omitDeep(node, path, index + 1))
        else if (node is JSONArray)
            dict.put(path[index], omitDeep(node, path, index + 1))
    } else
        dict.remove(path[index])
    return dict
}

fun omitDeep(dict: JSONArray, path: List<String>, index: Int): JSONArray {
    for (i in 0..<dict.length())
        dict.put(i, omitDeep(dict.getJSONObject(i), path, index))
    return dict
}

fun <T> compare(
    name: String,
    fromJson: (JSONObject) -> T,
    toJson: (T, Context) -> JSONObject?,
    vararg omit: String
) {
    var expected = readFile(name)
    for (key in omit) expected = omitDeep(expected, key.split("."), 0)
    val actual = toJson(fromJson(expected), ApplicationProvider.getApplicationContext())!!
    compareJSONs(name, expected, actual)
}

fun floatToDouble(input: JSONObject): JSONObject {
    for (key in input.keys()) {
        val value = input.get(key)
        if (value is JSONObject) input.put(key, floatToDouble(value))
        if (value is JSONArray) input.put(key, floatToDouble(value))
        if (value is Float) input.put(key, value.toString().toDouble())
    }
    return input
}

fun floatToDouble(input: JSONArray): JSONArray {
    for (i in 0..<input.length()) {
        val value = input.get(i)
        if (value is JSONObject) input.put(i, floatToDouble(value))
        if (value is JSONArray) input.put(i, floatToDouble(value))
        if (value is Float) input.put(i, value.toString().toDouble())
    }
    return input
}

@Suppress("unused", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
internal object Convert {
    fun byteArrayFromBase64(base64: String?) = base64?.let { Base64.getDecoder().decode(it) }
    fun generateByteArray(array: ByteArray?) = array?.let { Base64.getEncoder().encodeToString(it) }

    fun bitmapFromBase64(base64: String?) = base64?.let {
        val bitmap = Shadow.newInstanceOf(Bitmap::class.java)
        val shadow = Shadow.extract<MyShadowBitmap>(bitmap)
        shadow.data = byteArrayFromBase64(base64)
        bitmap
    }

    fun bitmapToBase64(bitmap: Bitmap?) = bitmap?.let {
        val shadow = Shadow.extract<MyShadowBitmap>(bitmap)
        generateByteArray(shadow.data)
    }

    fun Any?.toDrawable(context: Context) = (this as String?)?.let {
        val bitmap = Shadow.newInstanceOf(BitmapDrawable::class.java)
        val shadow = Shadow.extract<MyShadowBitmapDrawable>(bitmap)
        shadow.data = byteArrayFromBase64(it)
        bitmap
    }

    fun Drawable?.toString() = this?.let {
        val shadow = Shadow.extract<MyShadowDrawable>(this)
        generateByteArray(shadow.data)
    }
}