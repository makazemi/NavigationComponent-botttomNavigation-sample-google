package com.maryam.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maryam.navigation.formscreen.Register
import com.maryam.navigation.homescreen.About
import com.maryam.navigation.listscreen.UserProfile

/**
 * An activity that inflates a layout that has a [BottomNavigationView].
 */
class MainActivity : AppCompatActivity() ,BottomNavController.NavGraphProvider,
    BottomNavController.OnNavigationGraphChanged,
    BottomNavController.OnNavigationReselectedListener {

    private val bottomNavController by lazy(LazyThreadSafetyMode.NONE) {
        BottomNavController(
            this,
            R.id.main_fragments_container,
            R.id.home,
            this,
        this)
    }

    override fun onGraphChange() {
    }

    override fun onReselectNavItem(
        navController: NavController,
        fragment: Fragment
    ){
        when(fragment){

            is UserProfile -> {
                navController.navigate(R.id.action_userProfile_to_leaderboard)
            }

            is About -> {
                navController.navigate(R.id.action_aboutScreen_to_titleScreen)
            }

            is Register -> {
                navController.navigate(R.id.action_registered_to_register)
            }
            else -> {
                // do nothing
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupBottomNavigationView(savedInstanceState)
    }

    private fun setupBottomNavigationView(savedInstanceState: Bundle?){
       val  bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.setUpNavigation(bottomNavController, this)
        if (savedInstanceState == null) {
            bottomNavController.setupBottomNavigationBackStack(null)
            bottomNavController.onNavigationItemSelected()
        }
        else{
            (savedInstanceState[BOTTOM_NAV_BACKSTACK_KEY] as IntArray?)?.let { items ->
                val backstack = BottomNavController.BackStack()
                backstack.addAll(items.toTypedArray())
                bottomNavController.setupBottomNavigationBackStack(backstack)
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // save backstack for bottom nav
        outState.putIntArray(BOTTOM_NAV_BACKSTACK_KEY, bottomNavController.navigationBackStack.toIntArray())
    }

    override fun onBackPressed() = bottomNavController.onBackPressed()


    override fun getNavGraphId(itemId: Int): Int  = when(itemId){
        R.id.home -> {
            R.navigation.home
        }
        R.id.form -> {
            R.navigation.form
        }
        R.id.list -> {
            R.navigation.list
        }
        else -> {
            R.navigation.home
        }
    }
}