package com.maryam.navigation

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import android.util.SparseArray
import androidx.annotation.IdRes
import androidx.annotation.NavigationRes
import androidx.core.util.set
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.parcel.Parcelize

/**
 * Manages the various graphs needed for a [BottomNavigationView].
 *
 * This sample is a workaround until the Navigation Component supports multiple back stacks.
 */
const val BOTTOM_NAV_BACKSTACK_KEY =
    "com.codingwithmitch.openapi.util.BottomNavController.bottom_nav_backstack"
const val TAG = "NavigationExtensions"

class BottomNavController(
    val navGraphIds: List<Int>,
    val fragmentManager: FragmentManager,
    val containerId: Int,
    @IdRes val appStartDestinationId: Int,
    val intent: Intent,
    val activity: Activity,
    val navGraphProvider: NavGraphProvider
) {
    lateinit var navItemChangeListener: OnNavigationItemChanged
    lateinit var navigationBackStack: BackStack

    // Map of tags
    val graphIdToTagMap = SparseArray<String>()

    // Result. Mutable live data with the selected controlled
    val selectedNavController = MutableLiveData<NavController>()

    var firstFragmentGraphId = 0

    // Now connect selecting an item with swapping Fragments
    var selectedItemTag: String = ""

    var firstFragmentTag: String = ""


    fun setupBottomNavigationBackStack(previousBackStack: BackStack?) {
        navigationBackStack = previousBackStack?.let {
            it
        } ?: BackStack.of(appStartDestinationId)
    }

    fun initSetupGraph(selectedItemId: Int) {
        // First create a NavHostFragment for each NavGraph ID

        navGraphIds.forEachIndexed { index, navGraphId ->
            val fragmentTag = getFragmentTag(index)

            // Find or create the Navigation host fragment
            val navHostFragment = obtainNavHostFragment(
                fragmentManager,
                fragmentTag,
                navGraphId,
                containerId
            )

            // Obtain its id
            val graphId = navHostFragment.navController.graph.id

            if (index == 0) {
                firstFragmentGraphId = graphId
            }

            // Save to the map
            graphIdToTagMap[graphId] = fragmentTag

            // Attach or detach nav host fragment depending on whether it's the selected item.
            if (selectedItemId == graphId) {
                // Update livedata with the selected graph
                selectedNavController.value = navHostFragment.navController
                attachNavHostFragment(fragmentManager, navHostFragment, index == 0)
            } else {
                detachNavHostFragment(fragmentManager, navHostFragment)
            }

            selectedItemTag = graphIdToTagMap[selectedItemId]

            firstFragmentTag = graphIdToTagMap[firstFragmentGraphId]
        }

    }

    // For setting the checked icon in the bottom nav
    interface OnNavigationItemChanged {
        fun onItemChanged(itemId: Int)
    }

    fun setOnItemNavigationChanged(listener: (itemId: Int) -> Unit) {
        this.navItemChangeListener = object : OnNavigationItemChanged {
            override fun onItemChanged(itemId: Int) {
                listener.invoke(itemId)
            }
        }
    }

    fun onNavigationItemSelected(menuItemId: Int = navigationBackStack.last()): Boolean {


        val newlySelectedItemTag = graphIdToTagMap[menuItemId]
        Log.d("NavigationExtensions", "newlySelectedItemTag=$newlySelectedItemTag ")

        selectedItemTag = newlySelectedItemTag

        val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                as NavHostFragment

        val fragment = fragmentManager.findFragmentByTag(menuItemId.toString())
            ?: NavHostFragment.create(navGraphProvider.getNavGraphId(menuItemId))
        fragmentManager.beginTransaction()
            .replace(containerId, fragment, menuItemId.toString())
            .addToBackStack(null)
            .commit()


        selectedNavController.value = selectedFragment.navController


        // Add to back stack
        navigationBackStack.moveLast(menuItemId)


        // Update checked icon
        navItemChangeListener.onItemChanged(menuItemId)

        return true
    }


    fun onBack() {
        when {
            fragmentManager.findFragmentById(containerId)?.childFragmentManager!!.popBackStackImmediate() -> {

                Log.d(TAG, "state1")
            }

            // Fragment back stack is empty so try to go back on the navigation stack
            navigationBackStack.size > 1 -> {

                // Remove last item from back stack

                Log.d(TAG, "state2")
                navigationBackStack.removeLast()

                // Update the container with new fragment
                onNavigationItemSelected()
            }
            // If the stack has only one and it's not the navigation home we should
            // ensure that the application always leave from startDestination
            navigationBackStack.last() != appStartDestinationId -> {
                Log.d(TAG, "state3")
                navigationBackStack.removeLast()
                navigationBackStack.add(0, appStartDestinationId)
                onNavigationItemSelected()
            }
            // Navigation stack is empty, so finish the activity
            else -> {
                Log.d(TAG, "state4")
                activity.finish()
            }
        }
    }

    interface NavGraphProvider {
        @NavigationRes
        fun getNavGraphId(itemId: Int): Int
    }
}

fun BottomNavigationView.setupWithNavController(
    bottomNavController: BottomNavController
): LiveData<NavController> {


    bottomNavController.initSetupGraph(this.selectedItemId)
    // When a navigation item is selected
    setOnNavigationItemSelectedListener { item ->
        bottomNavController.onNavigationItemSelected(item.itemId)
    }

    // Optional: on item reselected, pop back stack to the destination of the graph
    setupItemReselected(bottomNavController.graphIdToTagMap, bottomNavController.fragmentManager)

    // Handle deep link
    setupDeepLinks(
        bottomNavController.navGraphIds,
        bottomNavController.fragmentManager,
        bottomNavController.containerId,
        bottomNavController.intent
    )

    // Finally, ensure that we update our BottomNavigationView when the back stack changes

    bottomNavController.setOnItemNavigationChanged { itemId ->
        menu.findItem(itemId).isChecked = true
    }

    return bottomNavController.selectedNavController
}

private fun BottomNavigationView.setupDeepLinks(
    navGraphIds: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int,
    intent: Intent
) {
    navGraphIds.forEachIndexed { index, navGraphId ->
        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment = obtainNavHostFragment(
            fragmentManager,
            fragmentTag,
            navGraphId,
            containerId
        )
        // Handle Intent
        if (navHostFragment.navController.handleDeepLink(intent)
            && selectedItemId != navHostFragment.navController.graph.id
        ) {
            this.selectedItemId = navHostFragment.navController.graph.id
        }
    }
}

private fun BottomNavigationView.setupItemReselected(
    graphIdToTagMap: SparseArray<String>,
    fragmentManager: FragmentManager
) {
    setOnNavigationItemReselectedListener { item ->
        val newlySelectedItemTag = graphIdToTagMap[item.itemId]
        val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                as NavHostFragment
        val navController = selectedFragment.navController
        // Pop the back stack to the start destination of the current navController graph
        navController.popBackStack(
            navController.graph.startDestination, false
        )
    }
}

private fun detachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment
) {
    fragmentManager.beginTransaction()
        .detach(navHostFragment)
        .commitNow()
}

private fun attachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment,
    isPrimaryNavFragment: Boolean
) {
    fragmentManager.beginTransaction()
        .attach(navHostFragment)
        .apply {
            if (isPrimaryNavFragment) {
                setPrimaryNavigationFragment(navHostFragment)
            }
        }
        .commitNow()

}

private fun obtainNavHostFragment(
    fragmentManager: FragmentManager,
    fragmentTag: String,
    navGraphId: Int,
    containerId: Int
): NavHostFragment {
    // If the Nav Host fragment exists, return it
    val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
    existingFragment?.let { return it }

    // Otherwise, create it and return it.
    val navHostFragment = NavHostFragment.create(navGraphId)
    fragmentManager.beginTransaction()
        .add(containerId, navHostFragment, fragmentTag)
        .commitNow()
    return navHostFragment
}

private fun getFragmentTag(index: Int) = "bottomNavigation#$index"

@Parcelize
class BackStack : ArrayList<Int>(), Parcelable {

    companion object {

        fun of(vararg elements: Int): BackStack {
            val b = BackStack()
            b.addAll(elements.toTypedArray())
            return b
        }
    }

    fun removeLast() = removeAt(size - 1)

    fun moveLast(item: Int) {
        remove(item) // if present, remove
        add(item) // add to end of list
    }

}