package io.kotlintest.show

import java.util.*
import kotlin.reflect.full.memberProperties

fun Any?.show() = Show(this).show(this)

interface Show<in A> {
  fun show(a: A): String
  fun supports(a: Any?): Boolean

  @Suppress("UNCHECKED_CAST")
  companion object {

    private fun <T> fromServiceLoader(t: T): Show<T>? =
        ServiceLoader.load(Show::class.java).toList().find { it.supports(t) } as? Show<T>

    private fun <T> forDataClass(t: T): Show<T>? =
        if ((t as? Any)?.javaClass?.kotlin?.isData == true) DataClassShow as Show<T> else null

    operator fun <T> invoke(t: T): Show<T> = when (t) {
      null -> NullShow as Show<T>
      is String -> StringShow as Show<T>
      else -> {
        val showt = fromServiceLoader(t)
        if (showt == null) AnyShow else showt as Show<T>
      }
    }
  }
}

object StringShow : Show<String> {
  override fun supports(a: Any?): Boolean = a is String
  override fun show(a: String): String = when (a) {
    "" -> "<empty string>"
    else -> a
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\'", "\\\'")
      .replace("\t", "\\\t")
      .replace("\b", "\\\b")
      .replace("\n", "\\\n")
      .replace("\r", "\\\r")
      .replace("\$", "\\\$")
  }
}

object AnyShow : Show<Any?> {
  override fun supports(a: Any?): Boolean = true
  override fun show(a: Any?): String = when (a) {
    null -> "<null>"
    else -> a.toString()
  }
}

object DataClassShow : Show<Any> {
  override fun supports(a: Any?): Boolean = a?.javaClass?.kotlin?.isData ?: false
  override fun show(a: Any): String {
    val klass = a.javaClass.kotlin
    require(klass.isData)
    return "${klass.simpleName}(\n" +
        klass.memberProperties.joinToString("\n") {
          "- ${it.name}: ${it.get(a)}"
        } + "\n)"
  }
}

object NullShow : Show<Nothing?> {
  override fun supports(a: Any?): Boolean = a == null
  override fun show(a: Nothing?): String = "<null>"
}