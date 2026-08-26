package com.jyco.smarttransfer.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

//@Preview
@Composable
//fun PinEntry(){
fun PinEntry(modifier: Modifier = Modifier, pin:String,
             onPinChanged:(filteredPin:String)->Unit, onSubmit:()->Unit ){
    //var pin = "123456"
    val focusRequester = remember{FocusRequester()}
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(pin) {
        if(pin.length == 6){
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally){
        BasicTextField(
            value = pin,
            onValueChange = { newValue->
                val filtered = newValue.filter { it.isDigit() }.take(6)
                onPinChanged(filtered)
            },
            modifier = modifier.focusRequester(focusRequester),
            decorationBox = {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)){
                    repeat(6){index ->
                        val isCurrentIndex = if(index == pin.length) true else false
                        Box(modifier = Modifier
                            .size(
                                width = 44.dp,
                                height = 52.dp
                            )
                            .border(
                                width = if (isCurrentIndex) 1.dp else 2.dp,
                                color = if (isCurrentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(2.dp)
                            .clickable {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        ,
                            contentAlignment = Alignment.Center
                        ){
                            Text(text= pin.getOrNull(index)?.toString() ?: "", style = MaterialTheme.typography.titleLarge
                            , textAlign = TextAlign.Center)
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.error)
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onSubmit,
            enabled = pin.length == 6,
            modifier = Modifier
                .width(180.dp)
                .height(52.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Submit",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

}

