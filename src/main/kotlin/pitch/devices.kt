package pitch

import javafx.collections.FXCollections
import tornadofx.*
import javax.sound.midi.*
import javax.sound.midi.ShortMessage.NOTE_OFF
import javax.sound.midi.ShortMessage.NOTE_ON

class DevicesController : Controller() {
    val inputDevices = FXCollections.observableArrayList<MidiDevice>()
    val outputDevices = FXCollections.observableArrayList<MidiDevice>()

    private var inputDevice: MidiDevice? = null
    private var outputDevice: MidiDevice? = null

    private var noteProcessor = NoteProcessor()

    fun changeInputDevice(device: MidiDevice?) {
        if (device == inputDevice) return
        inputDevice?.close()

        device?.open()
        device?.transmitter?.receiver = MidiInputReceiver(device?.deviceInfo.toString())

        inputDevice = device
    }

    fun changeOutputDevice(device: MidiDevice?) {
        if (device == outputDevice) return
        outputDevice?.close()

        device?.open()

        outputDevice = device
    }

    fun reset() {
        inputDevice?.close()
        outputDevice?.close()
    }

    fun reloadDevices() {
        println("Hello World!")

        var device: MidiDevice?
        var infos = MidiSystem.getMidiDeviceInfo()
        val newDevices = mutableListOf<MidiDevice>();
        for (midiSystem in infos) {
            device = MidiSystem.getMidiDevice(midiSystem)
            newDevices.add(device)
            //does the device have any transmitters?
            //if it does, add it to the device list
            println(midiSystem)
        }

        if (inputDevice != null) {
            try {
                changeInputDevice(newDevices.first { it.deviceInfo.name.equals(inputDevice!!.deviceInfo.name) })
            } catch (ex: NoSuchElementException) {
                changeInputDevice(null)
            }
        }

        if (outputDevice != null) {
            try {
                changeOutputDevice(newDevices.first { it.deviceInfo.name.equals(outputDevice!!.deviceInfo.name) })
            } catch (ex: NoSuchElementException) {
                changeOutputDevice(null)
            }
        }

        inputDevices.clear()
        inputDevices.addAll(newDevices.filter { it.maxTransmitters == -1 })

        outputDevices.clear()
        outputDevices.addAll(newDevices.filter { it.maxReceivers == -1 })
    }
}

open class MidiInputReceiver(private val name: String, private val noteProcessor: NoteProcessor) : Receiver {

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
