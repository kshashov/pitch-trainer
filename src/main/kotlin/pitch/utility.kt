package pitch

import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

fun <I> listViewCellRenderer(valueProvider: (I) -> String): Callback<ListView<I>, ListCell<I>> =
    Callback {
        object : ListCell<I>() {
            override fun updateItem(item: I?, empty: Boolean) {
                super.updateItem(item, empty)

                text = if (empty || (item == null)) null else valueProvider.invoke(item)
            }
        }
    }
