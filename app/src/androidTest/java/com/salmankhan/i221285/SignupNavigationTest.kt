package com.salmankhan.i221285

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignupNavigationTest {

    @get:Rule
    var activityRule = ActivityScenarioRule(SignupActivity::class.java)

    @Test
    fun testNavigateToSwitchAccountsViaCreateAccount() {
        // Click "Create Account" button to navigate to SwitchAccountsActivity
        onView(withId(R.id.createAccountButton)).perform(click())

        // Verify SwitchAccountsActivity is displayed (check for a visible element, e.g., log_in_button)
        onView(withId(R.id.log_in_button)).check(matches(isDisplayed()))
    }
}