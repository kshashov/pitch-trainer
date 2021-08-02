package pitch

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.media.AudioClip
import javafx.scene.text.Font
import javafx.util.StringConverter
import tornadofx.*
import javax.sound.midi.MidiDevice
import javax.sound.midi.ShortMessage
import javax.sound.midi.ShortMessage.NOTE_OFF
import javax.sound.midi.ShortMessage.NOTE_ON


class MyApp : App(SettingsView::class)

// TODO handle device errors on open and play
// TODO ?? forbid changes after set
// TODO reset excercise if settings are changed
// TODO Show streak and graph
// TODO Store configuration in file

class SettingsView : View() {
    private val controller: MainController by inject()
    private val devicesController: DevicesController by inject()

    init {
        devicesController.reloadDevices()
        controller.devicesController = devicesController
    }

    val deviceCellRenderer = simpleCellRenderer<MidiDevice?> { it!!.deviceInfo.name }
    val noteCellRenderer = simpleCellRenderer<Note?> { it!!.title }
    val octaveRenderer = simpleCellRenderer<Int?> { it!!.toString() }

    override val root = borderpane {

        left = form {
            maxWidth = 350.0
            fieldset("MIDI Devices") {
                field("Input") {
                    combobox(devicesController.inputDeviceProperty()) {
                        useMaxWidth = true
                        cellFactory = deviceCellRenderer
                        items = devicesController.inputDevices
                        buttonCell = deviceCellRenderer.call(null)
                    }
                    button("⟳") {
                        action { devicesController.reloadDevices() }
                    }
                }
                field("Output") {
                    combobox(devicesController.outputDeviceProperty()) {
                        useMaxWidth = true
                        cellFactory = deviceCellRenderer
                        items = devicesController.outputDevices
                        buttonCell = deviceCellRenderer.call(null)
                    }

                }
            }

            fieldset("Relative pitch mode") {
                val octaveRenderer = simpleCellRenderer<Int?>("From second note") { it!!.toString() }

                field("First note") {
                    combobox(controller.intervalNoteProperty()) {
                        useMaxWidth = true
                        cellFactory = noteCellRenderer
                        items = controller.intervalNotes
                        buttonCell = noteCellRenderer.call(null)
                    }
                    combobox(controller.intervalOctaveProperty()) {
                        useMaxWidth = true
                        cellFactory = octaveRenderer
                        items = controller.intervalOctaves
                        buttonCell = octaveRenderer.call(null)
                    }
                }
            }

            fieldset("Other") {

                field("Start note") {
                    combobox(controller.startNoteProperty()) {
                        useMaxWidth = true
                        cellFactory = noteCellRenderer
                        items = controller.notes
                        buttonCell = noteCellRenderer.call(null)
                    }
                    combobox(controller.startOctaveProperty()) {
                        useMaxWidth = true
                        cellFactory = octaveRenderer
                        items = controller.octaves
                        buttonCell = octaveRenderer.call(null)
                    }
                }

                field("End note") {
                    combobox(controller.endNoteProperty()) {
                        useMaxWidth = true
                        cellFactory = noteCellRenderer
                        items = controller.notes
                        buttonCell = noteCellRenderer.call(null)
                    }
                    combobox(controller.endOctaveProperty()) {
                        useMaxWidth = true
                        cellFactory = octaveRenderer
                        items = controller.octaves
                        buttonCell = octaveRenderer.call(null)
                    }
                }

                field("Notes") {
                    listview(controller.notes) {
                        multiSelect(true)
                        prefHeight = 200.0
                        useMaxWidth = true
                        cellFactory = noteCellRenderer
                        selectionModel.selectIndices(
                            -1,
                            *controller.allowedNotes.map { it.index }.toIntArray()
                        )

                        bindObservable(selectionModel.selectedItems, controller.allowedNotes)
                    }
                }

                field("Allow wrong octaves") {
                    checkbox("", controller.isWrongOctaveAllowedProperty())
                }
            }
        }
        center = vbox {
            prefWidth = 300.0
            alignment = Pos.CENTER
            label(
                observable = controller.guessNoteProperty(),
                converter = object : StringConverter<KeyboardNote?>() {
                    override fun toString(var1: KeyboardNote?): String? {
                        return var1?.note?.title + var1?.octave
                    }

                    override fun fromString(var1: String?): KeyboardNote? {
                        return null
                    }
                }) {
                font = Font("Arial", 30.0)
            }
            label(
                observable = controller.currentNoteProperty(),
                converter = object : StringConverter<KeyboardNote?>() {
                    override fun toString(var1: KeyboardNote?): String? {
                        return var1?.note?.title + var1?.octave
                    }

                    override fun fromString(var1: String?): KeyboardNote? {
                        return null
                    }
                }) {
                font = Font("Arial", 15.0)
            }
        }
    }

    override fun onUndock() {
        super.onUndock()
        devicesController.dispose()
    }
}

class MainController : Controller() {
    lateinit var devicesController: DevicesController

    // Helpful dictionaries
    val intervalNotes = FXCollections.observableArrayList(Note.values().toMutableList())
    val intervalOctaves = FXCollections.observableArrayList((1..7).toList())
    val notes = FXCollections.observableArrayList(Note.values().toMutableList())
    val octaves = FXCollections.observableArrayList((1..7).toList())
    val allowedNotes = FXCollections.observableList(Note.values().toMutableList().filter { it.white }.toList())
    val allowedCodes = mutableListOf<Int>()

    // UI properties
    fun isWrongOctaveAllowedProperty() = getProperty(MainController::isDifferentOctaveAllowed)
    fun startNoteProperty() = getProperty(MainController::startNote)
    fun endNoteProperty() = getProperty(MainController::endNote)
    fun startOctaveProperty() = getProperty(MainController::startOctave)
    fun endOctaveProperty() = getProperty(MainController::endOctave)
    fun intervalNoteProperty() = getProperty(MainController::intervalNote)
    fun intervalOctaveProperty() = getProperty(MainController::intervalOctave)

    // Internal properties
    fun currentNoteProperty() = getProperty(MainController::currentNote) // Current random note from specified range
    fun guessNoteProperty() = getProperty(MainController::guessNote) // User guess

    private var isDifferentOctaveAllowed by property(false)
    private var startNote: Note? by property(Note.C)
    private var endNote: Note? by property(Note.B)
    private var startOctave: Int? by property(2)
    private var endOctave: Int? by property(3)
    private var intervalNote: Note? by property(Note.C)
    private var intervalOctave: Int? by property(null)

    private var currentNote: KeyboardNote? by property(null)
    private var guessNote: KeyboardNote? by property(null)

    private val successClip: AudioClip = AudioClip(resources["/media/success.wav"])
    private val faultClip: AudioClip = AudioClip(resources["/media/fault.wav"])

    init {
        intervalNotes.add(0, null)
        intervalOctaves.add(0, null)
        startNoteProperty().addListener(ChangeListener { _, _, newValue -> adjustAllowedCodes() })
        endNoteProperty().addListener(ChangeListener { _, _, newValue -> adjustAllowedCodes() })
        startOctaveProperty().addListener(ChangeListener { _, _, newValue -> adjustAllowedCodes() })
        endOctaveProperty().addListener(ChangeListener { _, _, newValue -> adjustAllowedCodes() })

        successClip.volume = 0.5
        faultClip.volume = 0.5

        adjustAllowedCodes()
    }

    private fun adjustAllowedCodes() {
        val startNote = startNote
        val endNote = endNote
        val startOctave = startOctave
        val endOctave = endOctave
        val allowedNotes = allowedNotes

        if (startNote == null || startOctave == null || endNote == null || endOctave == null) {
            return
        } else {
            allowedCodes.clear()

            val start = NoteProcessor.code(startNote, startOctave)
            val end = NoteProcessor.code(endNote, endOctave)

            for (note in allowedNotes) {
                for (octave in startOctave..endOctave) {

                    allowedCodes.add(NoteProcessor.code(note, octave))
                }
            }

            allowedCodes.removeIf { (it < start) || (it > end) }
        }
    }

    fun postNote(code: Int) {
        if (code == 21) { // TODO extract to settings
            val note = KeyboardNote.fromCode(wishNote())
            Platform.runLater {
                currentNoteProperty().set(note)
            }
            playNote(note)
        } else if (code == 23) { // TODO extract to settings
            val note = currentNoteProperty().get() ?: return


            val intervalNote = intervalNoteProperty().get()
            if (intervalNote != null) {
                var intervalOctave = intervalOctaveProperty().get()
                if (intervalOctave == null) {
                    intervalOctave = note.octave!! // Get octave from current note
                }
                playNote(KeyboardNote.fromNote(intervalNote, intervalOctave))
            }
            playNote(note)
        } else {
            Platform.runLater {
                guessNoteProperty().set(NoteProcessor.parseCode(code))
            }

            if (validateNote(code)) {
                successClip.play()
            } else {
                faultClip.play()
            }

        }
    }

    private fun validateNote(key: Int): Boolean {
        val note = currentNoteProperty().get() ?: return false

        return if (isWrongOctaveAllowedProperty().get()) {
            NoteProcessor.parseNote(key) == note.note
        } else {
            key == note.code
        }
    }

    private fun playNote(note: KeyboardNote) {
        if (note.code == null) return

        pitch.run {
            devicesController.outputDeviceProperty().get()!!.receiver.send(
                ShortMessage(NOTE_ON, 4, note.code, 93),
                -1
            )
            Thread.sleep(1000) // TODO extract to settings
            devicesController.outputDeviceProperty().get()!!.receiver.send(
                ShortMessage(NOTE_OFF, 4, note.code, 93),
                -1
            )
        }
    }

    private fun wishNote(): Int {
        return allowedCodes.random()
    }
}
