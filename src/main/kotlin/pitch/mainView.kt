package pitch

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import javafx.scene.media.AudioClip
import javafx.scene.text.Font
import javafx.util.StringConverter
import tornadofx.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.sound.midi.MidiDevice
import javax.sound.midi.ShortMessage
import javax.sound.midi.ShortMessage.NOTE_OFF
import javax.sound.midi.ShortMessage.NOTE_ON


class MyApp : App(SettingsView::class, Styles::class) {
    init {
//        reloadViewsOnFocus()
    }
}

// UX impovements
// TODO auto next play
// TODO Allow `play` only after validation
// TODO reset/next/repeat excercise buttons
// TODO Show streak and stats
// Relative mode impovements
// TODO Relative note - dont detect mistakes

class SettingsView : View("Pitch Trainer") {
    private val controller: MainController by inject()
    private val devicesController: DevicesController by inject()
    private val deviceCellRenderer = simpleCellRenderer<MidiDevice?> { it!!.deviceInfo.name }
    private val noteCellRenderer = simpleCellRenderer<Note?> { it!!.title }
    private val octaveRenderer = simpleCellRenderer<Int?> { it!!.toString() }

    init {
        devicesController.reloadDevices()
        controller.devicesController = devicesController
    }

    override val root = borderpane {
        addClass(Styles.app)

        left = form {
            prefWidth = 400.0
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
                val octaveRenderer = simpleCellRenderer<Int?>("From main note") { it!!.toString() }
                val simpleCellRenderer = simpleCellRenderer<RelativeMode> { it.title }

                field("Mode") {
                    combobox(controller.relativeModeProperty()) {
                        useMaxWidth = true
                        cellFactory = simpleCellRenderer
                        items = controller.relativeModes
                        buttonCell = simpleCellRenderer.call(null)
                    }
                }
                field("Note") {
                    combobox(controller.relativeNoteProperty()) {
                        useMaxWidth = true
                        cellFactory = noteCellRenderer
                        items = controller.relativeNotes
                        buttonCell = noteCellRenderer.call(null)
                    }
                    combobox(controller.relativeOctaveProperty()) {
                        useMaxWidth = true
                        cellFactory = octaveRenderer
                        items = controller.relativeOctaves
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
            val converter = object : StringConverter<KeyboardNote?>() {
                override fun toString(var1: KeyboardNote?): String? {
                    return if (var1 == null) "" else var1.note?.title + var1.octave
                }

                override fun fromString(var1: String?): KeyboardNote? {
                    return null
                }
            }
            prefWidth = 300.0
            alignment = Pos.CENTER
            label(observable = controller.guessNoteProperty(), converter = converter) {
                font = Font("Arial", 30.0)
            }
            label(observable = controller.currentNoteProperty(), converter = converter) {
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
    val relativeNotes = FXCollections.observableArrayList(Note.values().toMutableList())
    val relativeOctaves = FXCollections.observableArrayList((1..7).toList())
    val relativeModes = FXCollections.observableArrayList(RelativeMode.values().toMutableList())
    val notes = FXCollections.observableArrayList(Note.values().toMutableList())
    val octaves = FXCollections.observableArrayList((1..7).toList())

    val allowedNotes = FXCollections.observableList(mutableListOf<Note>())
    val allowedCodes = mutableListOf<Int>()

    // UI properties
    fun isWrongOctaveAllowedProperty() = getProperty(MainController::isWrongOctaveAllowed)
    fun startNoteProperty() = getProperty(MainController::startNote)
    fun endNoteProperty() = getProperty(MainController::endNote)
    fun startOctaveProperty() = getProperty(MainController::startOctave)
    fun endOctaveProperty() = getProperty(MainController::endOctave)
    fun relativeNoteProperty() = getProperty(MainController::relativeNote)
    fun relativeOctaveProperty() = getProperty(MainController::relativeOctave)
    fun relativeModeProperty() = getProperty(MainController::relativeMode)

    // Internal properties
    fun currentNoteProperty() = getProperty(MainController::currentNote) // Current random note from specified range
    fun guessNoteProperty() = getProperty(MainController::guessNote) // User guess

    private var isWrongOctaveAllowed by property(config.boolean("isWrongOctaveAllowed", true))
    private var startNote: Note by property(Note.valueOf(config.string("startNote", Note.C.toString())))
    private var endNote: Note by property(Note.valueOf(config.string("endNote", Note.B.toString())))
    private var startOctave: Int by property(config.int("startOctave", 2))
    private var endOctave: Int by property(config.int("startOctave", 3))
    private var relativeNote: Note by property(Note.valueOf(config.string("startNote", Note.C.toString())))
    private var relativeOctave: Int? by property(config.int("relativeOctave"))
    private var relativeMode: RelativeMode by property(
        RelativeMode.valueOf(
            config.string(
                "relativeMode",
                RelativeMode.ASCENDING.toString()
            )
        )
    )

    private var currentNote: KeyboardNote? by property(null)
    private var guessNote: KeyboardNote? by property(null)

    private val successClip: AudioClip = AudioClip(resources["/media/success.wav"])
    private val faultClip: AudioClip = AudioClip(resources["/media/fault.wav"])
    private val threadPool = ScheduledThreadPoolExecutor(1) // It is enough to play notes

    init {
        relativeNotes.add(0, null)
        relativeOctaves.add(0, null)


        val savedAllowedNotes = config.string("allowedNotes")?.split(',')?.map { Note.valueOf(it) }
        if (savedAllowedNotes != null) allowedNotes.setAll(savedAllowedNotes)

        startNoteProperty().addListener(ChangeListener { _, _, newValue ->
            config["startNote"] = newValue.toString()
            config.save()
            adjustAllowedCodes()
        })
        endNoteProperty().addListener(ChangeListener { _, _, newValue ->
            config["endNote"] = newValue.toString()
            config.save()
            adjustAllowedCodes()
        })
        startOctaveProperty().addListener(ChangeListener { _, _, newValue ->
            config["startOctave"] = newValue.toString()
            config.save()
            adjustAllowedCodes()
        })
        endOctaveProperty().addListener(ChangeListener { _, _, newValue ->
            config["startOctave"] = newValue.toString()
            config.save()
            adjustAllowedCodes()
        })
        allowedNotes.addListener(ListChangeListener {
            config["allowedNotes"] = allowedNotes.joinToString(",") {
                it.toString()
            }
            config.save()

            adjustAllowedCodes()
        })
        isWrongOctaveAllowedProperty().addListener(ChangeListener() { _, _, newValue ->
            config["isWrongOctaveAllowed"] = newValue.toString()
            config.save()
        })
        relativeNoteProperty().addListener(ChangeListener { _, _, newValue ->
            config["relativeNote"] = newValue.toString()
            config.save()
        })
        relativeOctaveProperty().addListener(ChangeListener { _, _, newValue ->
            if (newValue == null) config.remove("relativeOctave")
            else config["relativeOctave"] = newValue.toString()

            config.save()
        })

        relativeModeProperty().addListener(ChangeListener { _, _, newValue ->
            config["relativeMode"] = newValue.toString()
            config.save()
        })

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
            play(note)
        } else if (code == 23) { // TODO extract to settings
            val note = currentNoteProperty().get() ?: return

            play(note)
        } else {
            Platform.runLater {
                guessNoteProperty().set(NoteProcessor.parseCode(code))
            }

            // Skip validation if nothing to guess
            if (currentNoteProperty().get() == null) {
                return
            }

            if (validateNote(code)) {
                Platform.runLater {
                    currentNoteProperty().set(null)
                }
                successClip.play()
            } else {
//                faultClip.play()
            }
        }
    }

    private fun play(note: KeyboardNote) {
        val relativeMode = relativeModeProperty().get()

        if (relativeMode == RelativeMode.NONE) {
            playNote(500L, note)
        } else {
            // Relative mode
            val relativeNote = relativeNoteProperty().get()

            var relativeOctave = relativeOctaveProperty().get()
            if (relativeOctave == null) {
                relativeOctave = note.octave!! // Get octave from current note
            }

            var first = KeyboardNote.fromNote(relativeNote, relativeOctave)
            var second = note

            val ascending = second.note!!.index >= first.note!!.index

            if (((relativeMode == RelativeMode.ASCENDING) && ascending)
                || ((relativeMode == RelativeMode.DESCENDING) && !ascending)
            ) {
                playNote(500L, first)
                playNote(1700L, second)
            } else {
                playNote(500L, second)
                playNote(1700L, first)
            }
        }
    }

    private fun playNote(initialDelay: Long, note: KeyboardNote) {
        if (note.code == null) return

        try {
            threadPool.schedule({
                devicesController.outputDeviceProperty().get()!!.receiver.send(
                    ShortMessage(NOTE_ON, 4, note.code, 93),
                    -1
                )
            }, initialDelay, TimeUnit.MILLISECONDS)

            threadPool.schedule({
                devicesController.outputDeviceProperty().get()!!.receiver.send(
                    ShortMessage(NOTE_OFF, 4, note.code, 93),
                    -1
                )
            }, initialDelay + 1000L, TimeUnit.MILLISECONDS)
        } catch (ex: Exception) {
            error("Looks like something wrong with your MIDI device. Please check the settings or reopen the app.")
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

    private fun error(text: String) {
        Alert(
            AlertType.ERROR,
            text,
            ButtonType.OK,
        ).showAndWait()
    }

    private fun wishNote(): Int {
        return allowedCodes.random()
    }
}

enum class RelativeMode(val title: String) {
    NONE("Off"), ASCENDING("Ascending"), DESCENDING("Descending"), RANDOM("Random")
}
