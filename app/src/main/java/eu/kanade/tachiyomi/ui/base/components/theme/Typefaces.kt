package eu.kanade.tachiyomi.ui.base.components.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R

object Typefaces {

    val montserrat = FontFamily(
        Font(R.font.montserrat_thin, FontWeight.Thin),
        Font(R.font.montserrat_black, FontWeight.Black),
        Font(R.font.montserrat_bold, FontWeight.Bold),
        Font(R.font.montserrat_extra_bold, FontWeight.ExtraBold),
        Font(R.font.montserrat_medium, FontWeight.Medium),
        Font(R.font.montserrat_semi_bold, FontWeight.SemiBold),
        Font(R.font.montserrat_regular, FontWeight.Normal),

        )

    val body = 16.sp
}