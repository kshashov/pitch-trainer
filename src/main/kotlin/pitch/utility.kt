package pitch

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback


fun <I> simpleCellRenderer(emptyText: String = "", valueProvider: (I) -> String): Callback<ListView<I>, ListCell<I>> =
    Callback {
        object : ListCell<I>() {
            override fun updateItem(item: I?, empty: Boolean) {
                super.updateItem(item, empty)
                text = if (empty || (item == null)) emptyText else valueProvider.invoke(item)
            }
        }
    }

fun <T> bindObservable(source: ObservableList<T>, target: ObservableList<T>): ObservableList<T> {
    source.addListener { c: ListChangeListener.Change<out T> ->
        // TODO MINOR use wasAdded, wasRemoved, etc methods
        target.setAll(c.list)
    }
    return target
}

fun run(delay: Long = 500, runnable: Runnable) {
    val sleeper: Task<Void?> = object : Task<Void?>() {
        override fun call(): Void? {
            try {
                Thread.sleep(delay)
            } catch (e: InterruptedException) {
            }
            return null
        }
    }
    sleeper.setOnSucceeded { runnable.run() }
    Thread(sleeper).start()
}
