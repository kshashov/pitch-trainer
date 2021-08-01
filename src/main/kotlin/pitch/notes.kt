package pitch

enum class Note(val title: String, val index: Int) {
    C("C", 1),
    Csh("C#", 2),
    D("D", 3),
    Dsh("D#", 4),
    E("E", 5),
    F("F", 6),
    Fsh("F#", 7),
    G("G", 8),
    Gsh("G#", 9),
    A("A", 10),
    Ash("A#", 11),
    B("B", 12)
}

class NoteProcessor {
    companion object {
        private var cache: Map<Int, Note> = Note.values().associateBy { note -> note.index }

        fun parseCode(code: Int): Note {
            val octave = code / 12 - 1
            val index = code % 12
            return cache[index]!!
        }
    }
}
