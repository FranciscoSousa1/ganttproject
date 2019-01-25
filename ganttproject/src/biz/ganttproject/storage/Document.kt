/*
Copyright 2018 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.storage

import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableObjectValue
import net.sourceforge.ganttproject.document.Document
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

/**
 * @author dbarashev@bardsoftware.com
 */
class DocumentUri(private val components: List<String>,
                  private val isAbsolute: Boolean = true,
                  private val root: String = "/") {

  fun isAbsolute(): Boolean {
    return this.isAbsolute
  }

  fun getNameCount(): Int {
    return this.components.size
  }

  fun subpath(start: Int, end: Int): DocumentUri {
    val resultAbsolute = if (start == 0) this.isAbsolute else false
    return DocumentUri(this.components.subList(start, end), resultAbsolute, this.root)
  }

  fun getRoot(): DocumentUri {
    return DocumentUri(listOf(), true, this.root)
  }

  fun getName(idx: Int): String {
    return this.components[idx]
  }

  fun getFileName(): String {
    return this.components.last()
  }

  fun getParent(): DocumentUri {
    if (this.components.isEmpty()) {
      return this
    }
    return DocumentUri(this.components.subList(0, this.components.size - 1), this.isAbsolute, this.root)
  }

  fun resolve(name: String): DocumentUri {
    if (name == "" || name == ".") {
      return this
    }
    if (name == "..") {
      return getParent()
    }
    return DocumentUri(this.components.toMutableList().apply {
      add(name)
      toList()
    }, this.isAbsolute, this.root)
  }

  fun resolve(path: DocumentUri): DocumentUri {
    if (path.isAbsolute) {
      return path
    }
    var result = this
    for (idx in 0 until path.getNameCount()) {
      result = result.resolve(path.getName(idx))
    }
    return result
  }

  fun normalize(): DocumentUri {
    return this.getRoot().resolve(DocumentUri(this.components, false, this.root))
  }

  fun getRootName(): String {
    return this.root
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DocumentUri

    if (components != other.components) return false
    if (isAbsolute != other.isAbsolute) return false
    if (root != other.root) return false

    return true
  }

  override fun hashCode(): Int {
    var result = components.hashCode()
    result = 31 * result + isAbsolute.hashCode()
    result = 31 * result + root.hashCode()
    return result
  }


  companion object LocalDocument {
    fun toFile(path: DocumentUri): File {
      val filePath = java.nio.file.Paths.get(path.root, *path.components.toTypedArray())
      return filePath.toFile()
    }

    fun createPath(file: File): DocumentUri {
      return createPath(file.toPath())
    }

    fun createPath(pathAsString: String): DocumentUri {
      return createPath(Paths.get(pathAsString))
    }

    private fun createPath(filePath: java.nio.file.Path): DocumentUri {
      val isAbsolute = filePath.isAbsolute
      val root = if (filePath.isAbsolute) filePath.root.toString() else ""

      val components = mutableListOf<String>()
      for (idx in 0 until filePath.nameCount) {
        components.add(filePath.getName(idx).toString())
      }
      return DocumentUri(components, isAbsolute, root)
    }
  }
}

data class LockStatus(val locked: Boolean,
                      val lockOwnerName: String? = null,
                      val lockOwnerEmail: String? = null,
                      val lockOwnerId: String? = null)
interface LockableDocument {
  fun toggleLocked(): CompletableFuture<LockStatus>

  val status: ObservableObjectValue<LockStatus>
}

class NetworkUnavailableException(cause: Exception) : RuntimeException(cause)

enum class OnlineDocumentMode {
  ONLINE_ONLY, MIRROR, OFFLINE_ONLY
}
interface OnlineDocument {
  var offlineMirror: Document?
  val isAvailableOffline: ObservableBooleanValue
  fun toggleAvailableOffline()
  var mode: OnlineDocumentMode
}

