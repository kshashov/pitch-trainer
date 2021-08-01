package pitch

import tornadofx.*
import javax.sound.midi.MidiDevice

class MyApp : App(MyView::class)

class MyView : View() {
    private val controller: MainController by inject()
    private val devicesController: DevicesController by inject()

    override val root = vbox {
        button("Reload") { action { devicesController.reloadDevices() } }
        label("My items")
        listview(devicesController.inputDevices) {
            cellFactory = listViewCellRenderer { it.deviceInfo.name }

            selectionModel.selectedItemProperty().addListener(ChangeListener<MidiDevice> { _, _, newValue ->
                devicesController.changeInputDevice(newValue)
            })
        }
        listview(devicesController.outputDevices) {
            cellFactory = listViewCellRenderer { it.deviceInfo.name }

            selectionModel.selectedItemProperty().addListener(ChangeListener<MidiDevice> { _, _, newValue ->
                devicesController.changeOutputDevice(newValue)
            })
        }

        checkbox("Allow different octaves", controller.isDifferentOctaveAllowedProperty())

    }
}

class MainController : Controller() {
    private var isDifferentOctaveAllowed by property(false)
    fun isDifferentOctaveAllowedProperty() = getProperty(MainController::isDifferentOctaveAllowed)

    fun postNote(key: Int) {
        TODO("Not yet implemented")
    }
}
