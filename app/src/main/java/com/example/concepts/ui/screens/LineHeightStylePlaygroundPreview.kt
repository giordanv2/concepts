package com.example.concepts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.concepts.R

@Composable
private fun LineHeightExample(
    label: String,
    alignment: Alignment
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color(0xFFEFEFEF))
        ) {
            Text(
                text = "equipaje EQUIPAJE\naaggsd",
                modifier = Modifier
                    .background(Color(0x5533AAFF)),
                style = TextStyle(
                    fontSize = 8.sp,
                    lineHeight = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeightStyle = LineHeightStyle(
                        alignment = alignment,
                        trim = Trim.None,
                    ),
                ),
                color = Color.Black
            )
        }
    }
}

val drSugiyama = FontFamily(
    Font(R.font.dr_sugiyama_regular)
)

@Preview(showBackground = true, name = "LineHeightStyle playground")
@Composable
fun LineHeightStylePlaygroundPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LineHeightExample(
            label = "Proportional",
            alignment = Alignment.Proportional
        )

        LineHeightExample(
            label = "Center",
            alignment = Alignment.Center
        )

        LineHeightExample(
            label = "Top",
            alignment = Alignment.Top
        )

        LineHeightExample(
            label = "Bottom",
            alignment = Alignment.Bottom
        )
    }
}