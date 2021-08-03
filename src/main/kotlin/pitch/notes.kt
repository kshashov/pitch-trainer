package pitch

import tornadofx.JsonModel

enum class Note(val title: String, val index: Int, val white: Boolean = true) : JsonModel {
    C("C", 0),
    Csh("C#", 1, false),
    D("D", 2),
    Dsh("D#", 3, false),
    E("E", 4),
    F("F", 5),
    Fsh("F#", 6, false),
    G("G", 7),
    Gsh("G#", 8, false),
    A("A", 9),
    Ash("A#", 10, false),
    B("B", 11)
}

class KeyboardNote(val note: Note?, val octave: Int?, val code: Int?) {

    companion object {
        fun fromCode(code: Int): KeyboardNote {
            val note = NoteProcessor.parseCode(code)
            return pitch.KeyboardNote(note.note, note.octave, note.code)
        }

        fun fromNote(note: Note, octave: Int): KeyboardNote {
            val code = NoteProcessor.code(note, octave)
            return pitch.KeyboardNote(note, octave, code)
        }

        fun empty(): KeyboardNote = pitch.KeyboardNote(null, null, -1)
    }
}

class NoteProcessor {
    companion object {
        private var cache: Map<Int, Note> = Note.values().associateBy { note -> note.index }

        fun parseNote(code: Int): Note {
            val octave = code / 12 - 1
            val index = code % 12
            return cache[index]!!
        }

        fun parseCode(code: Int): KeyboardNote {
            val octave = code / 12 - 1
            val index = code % 12
            return KeyboardNote.fromNote(cache[index]!!, octave)
        }

        fun code(note: Note, octave: Int): Int {
            var code = note.index + (octave - 1) * 12
            code += 24 // skip first 2 octaves
            return code
        }
    }
}
