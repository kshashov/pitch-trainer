package pitch

import javafx.scene.effect.DropShadow
import javafx.scene.paint.Color
import tornadofx.*

class Styles : Stylesheet() {
    companion object {
        val app by cssclass()

        private val shadowColor = Color.LIGHTGRAY
        private val cardBackColor = Color.WHITE
        private val appBackColor = c("#f4f4f4")
    }

    init {
        app {
            backgroundColor = multi(appBackColor)
        }

        fieldset {
//            rotate = 10.deg
//            borderColor += box(topColor,rightColor,bottomColor,leftColor)
//            fontFamily = "Comic Sans MS"
//            fontSize = 20.px
            backgroundColor += cardBackColor
            padding = box(15.px)
            backgroundRadius += box(5.px)
            backgroundInsets += box(5.px)
//            backgroundRadius += box(5.px)
            effect = DropShadow(20.0, shadowColor)
        }
    }
}
