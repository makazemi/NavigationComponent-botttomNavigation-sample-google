package com.maryam.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * An activity that inflates a layout that has a [BottomNavigationView].
 */
class MainActivity : AppCompatActivity() , BottomNavController.NavGraphProvider{

    private var currentNavController: LiveData<NavController>? = null


    private val bottomNavController by lazy(LazyThreadSafetyMode.NONE) {
        BottomNavController(
            navGraphIds = listOf(R.navigation.home, R.navigation.list, R.navigation.form),
            fragmentManager =supportFragmentManager,
            containerId = R.id.nav_host_container,
            appStartDestinationId = R.id.home,
            intent = intent,
            activity = this,
            navGraphProvider = this
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            bottomNavController.setupBottomNavigationBackStack(null)
        //    bottomNavController.onNavigationItemSelected()
        }
        else{
            (savedInstanceState[BOTTOM_NAV_BACKSTACK_KEY] as IntArray?)?.let { items ->
                val backstack = BackStack()
                backstack.addAll(items.toTypedArray())
                bottomNavController.setupBottomNavigationBackStack(backstack)
            }
        }


        setupBottomNavigationBar()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
                setupBottomNavigationBar()


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putIntArray(BOTTOM_NAV_BACKSTACK_KEY, bottomNavController.navigationBackStack.toIntArray())
    }

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

   val controller= bottomNavigationView.setupWithNavController(bottomNavController)

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(this, Observer { navController ->
            setupActionBarWithNavController(navController)
        })
        currentNavController = controller
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    override fun onBackPressed()  = bottomNavController.onBack()
    override fun getNavGraphId(itemId: Int): Int = when(itemId){
        R.id.home -> {
            R.navigation.home
        }
        R.id.list -> {
            R.navigation.list
        }
        R.id.form -> {
            R.navigation.form
        }
        else -> {
            R.navigation.home
        }
    }
}