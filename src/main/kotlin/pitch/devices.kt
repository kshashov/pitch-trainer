package pitch

import javafx.application.Platform
import javafx.collections.FXCollections
import tornadofx.ChangeListener
import tornadofx.Controller
import tornadofx.getProperty
import tornadofx.property
import javax.sound.midi.*
import javax.sound.midi.ShortMessage.NOTE_OFF
import javax.sound.midi.ShortMessage.NOTE_ON

class DevicesController : Controller() {
    val inputDevices = FXCollections.observableArrayList<MidiDevice>()
    val outputDevices = FXCollections.observableArrayList<MidiDevice>()

    fun inputDeviceProperty() = getProperty(DevicesController::inputDevice)
    fun outputDeviceProperty() = getProperty(DevicesController::outputDevice)

    private var inputDevice: MidiDevice? by property(null)
    private var outputDevice: MidiDevice? by property(null)
    private val mainController: MainController by inject()

    init {
        inputDeviceProperty().addListener(ChangeListener { _, oldValue, newValue ->
            changeInputDevice(
                oldValue,
                newValue
            )
        })
        outputDeviceProperty().addListener(ChangeListener { _, oldValue, newValue ->
            changeOutputDevice(
                oldValue,
                newValue
            )
        })
    }

    private fun changeInputDevice(old: MidiDevice?, device: MidiDevice?) {
        try {
            old?.close()
            device?.open()
            device?.transmitter?.receiver = MidiInputReceiver(device?.deviceInfo.toString(), mainController)
        } catch (ex: Exception) {
            Platform.runLater {
                inputDeviceProperty().set(old)
            }
        }
    }

    private fun changeOutputDevice(old: MidiDevice?, device: MidiDevice?) {
        try {
            old?.close()
            device?.open()
        } catch (ex: Exception) {
            Platform.runLater {
                outputDeviceProperty().set(old)
            }
        }
    }

    fun dispose() {
        inputDevice?.close()
        outputDevice?.close()
        inputDeviceProperty().set(null)
        outputDeviceProperty().set(null)
    }

    fun reloadDevices() {
        var device: MidiDevice?
        val infos = MidiSystem.getMidiDeviceInfo()
        val newDevices = mutableListOf<MidiDevice>();
        for (midiSystem in infos) {
            device = MidiSystem.getMidiDevice(midiSystem)
            newDevices.add(device)
        }

        val inputDevice = inputDeviceProperty().get()
        val outputDevice = outputDeviceProperty().get()

        inputDevices.setAll(newDevices.filter { it.maxTransmitters == -1 })
        outputDevices.setAll(newDevices.filter { it.maxReceivers == -1 })

        // Try to restore previous devices choise
        if (inputDevice != null) {
            try {
                inputDeviceProperty().set(
                    newDevices.first { it.deviceInfo.name.equals(inputDevice.deviceInfo.name) && (it.maxTransmitters == -1) })
            } catch (ex: NoSuchElementException) {
                // Do nothing as we already have null device after setAll
            }
        }

        if (outputDevice != null) {
            try {
                outputDeviceProperty().set(
                    newDevices.first { it.deviceInfo.name.equals(outputDevice.deviceInfo.name) && (it.maxReceivers == -1) })
            } catch (ex: NoSuchElementException) {
                // Do nothing as we already have null device after setAll
            }
        }
    }
}

open class MidiInputReceiver(private val name: String, private val noteProcessor: MainController) : Receiver {

    override fun send(msg: MidiMessage, timeStamp: Long) {
        println("midi received")
        if (msg is ShortMessage) {
            if (msg.command == NOTE_ON) {
                // ?

            } else if (msg.command == NOTE_OFF) {
                val key: Int = msg.data1
                noteProcessor.postNote(key)
            }
        }
    }

    override fun close() {}
}
