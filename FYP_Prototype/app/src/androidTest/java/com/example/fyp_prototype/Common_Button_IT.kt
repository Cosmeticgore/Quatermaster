package com.example.fyp_prototype

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class Common_Button_IT {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun button_common_on_click(){
        //arrange
        var click = false

        composeTestRule.setContent {
            button_common("TEST") {
                click = true
            }
        }

        //act

        composeTestRule.onNodeWithText("TEST").performClick()

        //assert
        assert(click)
        assertTrue(composeTestRule.onNodeWithText("TEST").isDisplayed())
    }

}